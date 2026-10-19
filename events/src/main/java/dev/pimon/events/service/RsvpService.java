package dev.pimon.events.service;

import dev.pimon.events.dto.RsvpDto;
import dev.pimon.events.dto.RsvpRequest;
import dev.pimon.events.dto.RsvpSummaryDto;
import dev.pimon.events.entity.Rsvp;
import dev.pimon.events.repository.RsvpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RsvpService {

    private final RsvpRepository repository;

    /** Registra una confirmación de asistencia. */
    @Transactional
    public RsvpDto create(RsvpRequest req) {
        Rsvp rsvp = new Rsvp();
        rsvp.setEventId(req.eventId());
        rsvp.setName(req.name());
        rsvp.setEmail(req.email());
        rsvp.setAttending(Boolean.TRUE.equals(req.attending()));
        // Los acompañantes solo cuentan si asiste.
        rsvp.setGuests(rsvp.isAttending() && req.guests() != null ? req.guests() : 0);
        rsvp.setMenuChoice(req.menuChoice());
        rsvp.setAllergies(req.allergies());
        rsvp.setSong(req.song());
        rsvp.setMessage(req.message());
        return RsvpDto.from(repository.save(rsvp));
    }

    /** Resumen agregado (confirmados, ausencias, total de asistentes y reparto por menú). */
    @Transactional(readOnly = true)
    public RsvpSummaryDto summary(String eventId) {
        List<Rsvp> all = repository.findByEventIdOrderByCreatedAtDesc(eventId);

        long confirmed = all.stream().filter(Rsvp::isAttending).count();
        long declined = all.size() - confirmed;
        // Asistentes = confirmados (cada uno cuenta como 1) + sus acompañantes.
        long totalGuests = all.stream().filter(Rsvp::isAttending).mapToLong(r -> 1L + r.getGuests()).sum();

        Map<String, Long> byMenu = all.stream()
                .filter(r -> r.isAttending() && r.getMenuChoice() != null && !r.getMenuChoice().isBlank())
                .collect(Collectors.groupingBy(Rsvp::getMenuChoice, Collectors.counting()));

        return new RsvpSummaryDto(eventId, confirmed, declined, totalGuests, byMenu);
    }

    /** Listado completo de confirmaciones de un evento (uso del organizador). */
    @Transactional(readOnly = true)
    public List<RsvpDto> list(String eventId) {
        return repository.findByEventIdOrderByCreatedAtDesc(eventId).stream()
                .map(RsvpDto::from)
                .toList();
    }
}
