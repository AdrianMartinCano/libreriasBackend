package dev.pimon.dining.service;

import dev.pimon.dining.dto.SeatingAvailabilityDto;
import dev.pimon.dining.entity.DiningReservation;
import dev.pimon.dining.entity.DiningTable;
import dev.pimon.dining.entity.Seating;
import dev.pimon.dining.repository.DiningReservationRepository;
import dev.pimon.dining.repository.DiningTableRepository;
import dev.pimon.dining.repository.SeatingRepository;
import dev.pimon.dining.settings.DiningSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcula qué mesas (o combinación de mesas) pueden acoger un grupo de comensales en un turno, y la
 * disponibilidad por turno de un día. A diferencia de las citas (¿cabe en la agenda de un recurso?),
 * aquí el problema es de <b>encaje de grupos en mesas</b> (bin-packing) con la capacidad medida en pax.
 *
 * <p>La asignación es <b>best-fit</b>: prefiere la mesa más pequeña que cubra el grupo (minimiza
 * sillas desperdiciadas); si ninguna sirve sola, combina mesas {@code combinable} de la misma zona con
 * una búsqueda greedy acotada (la mínima combinación que sume capacidad suficiente).</p>
 */
@Service
@RequiredArgsConstructor
public class TableAvailabilityService {

    private final DiningTableRepository       tableRepo;
    private final DiningReservationRepository reservationRepo;
    private final SeatingRepository           seatingRepo;
    private final DiningSettingsService       settingsService;

    /* ───────────────────────── Entrada pública ───────────────────────── */

    /**
     * Disponibilidad de cada turno aplicable a la fecha para un grupo de {@code party} comensales.
     * {@code zoneId} opcional acota a una zona. Si la fecha está cerrada, devuelve lista vacía.
     */
    @Transactional(readOnly = true)
    public List<SeatingAvailabilityDto> availability(LocalDate date, int party, String zoneId) {
        if (isClosed(date)) return List.of();

        List<DiningTable> tables = activeTables(zoneId);
        List<DiningReservation> active = reservationRepo.findActiveByDateWithTables(date);
        int totalCapacity = settingsService.current().totalCapacity();

        List<SeatingAvailabilityDto> result = new ArrayList<>();
        for (Seating seating : seatingRepo.findByActiveTrueOrderBySortOrderAscStartAsc()) {
            if (!seating.appliesTo(date.getDayOfWeek())) continue;

            List<DiningTable> free = freeTables(tables, seating, active);
            Optional<List<DiningTable>> assignment = findAssignment(free, party);
            boolean aforoOk = aforoFits(active, seating, party, totalCapacity);
            boolean available = assignment.isPresent() && aforoOk;

            result.add(new SeatingAvailabilityDto(
                    seating.getId(), seating.getName(), seating.getStart(), seating.getEnd(),
                    available,
                    available ? assignment.get().stream().map(DiningTable::getId).toList() : List.of()));
        }
        return result;
    }

    /* ───────────────────────── Helpers reutilizados por el alta ───────────────────────── */

    public boolean isClosed(LocalDate date) {
        List<String> blocked = settingsService.current().blockedDates();
        return blocked != null && blocked.contains(date.toString());
    }

    /** Mesas activas (con zona) opcionalmente filtradas por zona. */
    public List<DiningTable> activeTables(String zoneId) {
        return tableRepo.findActiveWithZone().stream()
                .filter(t -> zoneId == null || zoneId.isBlank() || t.getZone().getId().equals(zoneId))
                .toList();
    }

    /** Mesas que no están ocupadas por ninguna reserva activa cuyo turno solape al pedido. */
    public List<DiningTable> freeTables(List<DiningTable> tables, Seating seating,
                                        List<DiningReservation> active) {
        Set<String> occupied = occupiedTableIds(seating, active);
        return tables.stream().filter(t -> !occupied.contains(t.getId())).toList();
    }

    /** Ids de mesas tomadas por reservas activas cuyo turno solapa con {@code target}. */
    public Set<String> occupiedTableIds(Seating target, List<DiningReservation> active) {
        Set<String> occupied = new java.util.HashSet<>();
        for (DiningReservation r : active) {
            if (windowsOverlap(r.getSeating(), target)) {
                r.getTables().forEach(t -> occupied.add(t.getId()));
            }
        }
        return occupied;
    }

    /** ¿Cabe el grupo en el aforo global del turno? {@code totalCapacity <= 0} = sin tope. */
    public boolean aforoFits(List<DiningReservation> active, Seating seating, int party, int totalCapacity) {
        if (totalCapacity <= 0) return true;
        int seated = active.stream()
                .filter(r -> windowsOverlap(r.getSeating(), seating))
                .mapToInt(DiningReservation::getPartySize)
                .sum();
        return seated + party <= totalCapacity;
    }

    /**
     * Mejor asignación para un grupo entre las mesas libres dadas:
     * <ol>
     *   <li>mesa única que encaja {@code [min,max]} (best-fit: menor max);</li>
     *   <li>si no, mesa única con {@code max >= party} (ignora el min, para no bloquear grupos pequeños);</li>
     *   <li>si no, combinación mínima de mesas combinables de una misma zona que sume capacidad.</li>
     * </ol>
     * Devuelve vacío si el grupo no cabe de ninguna forma.
     */
    public Optional<List<DiningTable>> findAssignment(List<DiningTable> free, int party) {
        Comparator<DiningTable> byFit = Comparator
                .comparingInt(DiningTable::getCapacityMax)
                .thenComparingInt(DiningTable::getSortOrder);

        Optional<DiningTable> exact = free.stream().filter(t -> t.fits(party)).min(byFit);
        if (exact.isPresent()) return exact.map(List::of);

        Optional<DiningTable> byMax = free.stream().filter(t -> party <= t.getCapacityMax()).min(byFit);
        if (byMax.isPresent()) return byMax.map(List::of);

        return bestCombination(free, party);
    }

    /* ───────────────────────── Internos ───────────────────────── */

    /**
     * Combinación de mesas combinables de una misma zona cuya capacidad sumada cubra al grupo.
     * Greedy por zona (mesas grandes primero hasta sumar suficiente); entre zonas elige la combinación
     * con menos mesas y, a igualdad, la de menor capacidad sobrante.
     */
    private Optional<List<DiningTable>> bestCombination(List<DiningTable> free, int party) {
        Map<String, List<DiningTable>> byZone = free.stream()
                .filter(DiningTable::isCombinable)
                .collect(Collectors.groupingBy(t -> t.getZone().getId(), LinkedHashMap::new, Collectors.toList()));

        List<DiningTable> best = null;
        int bestWaste = Integer.MAX_VALUE;
        for (List<DiningTable> zoneTables : byZone.values()) {
            List<DiningTable> sorted = zoneTables.stream()
                    .sorted(Comparator.comparingInt(DiningTable::getCapacityMax).reversed())
                    .toList();
            List<DiningTable> combo = new ArrayList<>();
            int sum = 0;
            for (DiningTable t : sorted) {
                combo.add(t);
                sum += t.getCapacityMax();
                if (sum >= party) break;
            }
            if (sum < party) continue;   // ni juntando toda la zona cabe
            int waste = sum - party;
            boolean better = best == null
                    || combo.size() < best.size()
                    || (combo.size() == best.size() && waste < bestWaste);
            if (better) {
                best = combo;
                bestWaste = waste;
            }
        }
        return Optional.ofNullable(best);
    }

    /** Dos turnos del mismo día se solapan si sus ventanas [start,end) se intersecan. */
    private boolean windowsOverlap(Seating a, Seating b) {
        LocalTime aStart = LocalTime.parse(a.getStart());
        LocalTime aEnd   = LocalTime.parse(a.getEnd());
        LocalTime bStart = LocalTime.parse(b.getStart());
        LocalTime bEnd   = LocalTime.parse(b.getEnd());
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
