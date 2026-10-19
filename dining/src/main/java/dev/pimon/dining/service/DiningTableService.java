package dev.pimon.dining.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.dining.dto.DiningTableDto;
import dev.pimon.dining.dto.SaveTableRequest;
import dev.pimon.dining.entity.DiningTable;
import dev.pimon.dining.entity.Zone;
import dev.pimon.dining.repository.DiningReservationRepository;
import dev.pimon.dining.repository.DiningTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiningTableService {

    private final DiningTableRepository       repo;
    private final ZoneService                 zoneService;
    private final DiningReservationRepository reservationRepo;

    public List<DiningTableDto> listAll() {
        return repo.findActiveWithZone().stream().map(DiningTableDto::from).toList();
    }

    public List<DiningTableDto> listByZone(String zoneId) {
        return repo.findByZone_IdOrderBySortOrderAscLabelAsc(zoneId).stream()
                .map(DiningTableDto::from).toList();
    }

    @Transactional
    public DiningTableDto create(SaveTableRequest req) {
        validate(req);
        DiningTable t = new DiningTable();
        apply(t, req);
        return DiningTableDto.from(repo.save(t));
    }

    @Transactional
    public DiningTableDto update(String id, SaveTableRequest req) {
        validate(req);
        DiningTable t = findOrThrow(id);
        apply(t, req);
        return DiningTableDto.from(repo.save(t));
    }

    @Transactional
    public void toggleActive(String id) {
        DiningTable t = findOrThrow(id);
        t.setActive(!t.isActive());
        repo.save(t);
    }

    @Transactional
    public void delete(String id) {
        if (reservationRepo.existsByTables_Id(id)) {
            throw AppException.conflict("La mesa tiene reservas; desactívala en vez de borrarla.");
        }
        repo.delete(findOrThrow(id));
    }

    public DiningTable findOrThrow(String id) {
        return repo.findById(id).orElseThrow(() -> AppException.notFound("Mesa no encontrada: " + id));
    }

    private void validate(SaveTableRequest req) {
        if (req.capacityMin() > req.capacityMax()) {
            throw AppException.badRequest("La capacidad mínima no puede superar la máxima.");
        }
    }

    private void apply(DiningTable t, SaveTableRequest req) {
        Zone zone = zoneService.findOrThrow(req.zoneId());
        t.setLabel(req.label());
        t.setCapacityMin(req.capacityMin());
        t.setCapacityMax(req.capacityMax());
        t.setCombinable(req.combinable());
        t.setSortOrder(req.sortOrder());
        t.setPosX(req.posX());
        t.setPosY(req.posY());
        t.setZone(zone);
    }
}
