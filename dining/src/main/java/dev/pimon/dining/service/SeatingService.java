package dev.pimon.dining.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.dining.dto.SaveSeatingRequest;
import dev.pimon.dining.dto.SeatingDto;
import dev.pimon.dining.entity.Seating;
import dev.pimon.dining.repository.DiningReservationRepository;
import dev.pimon.dining.repository.SeatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatingService {

    private final SeatingRepository           repo;
    private final DiningReservationRepository reservationRepo;

    public List<SeatingDto> listActive() {
        return repo.findByActiveTrueOrderBySortOrderAscStartAsc().stream().map(SeatingDto::from).toList();
    }

    public List<SeatingDto> listAll() {
        return repo.findAllByOrderBySortOrderAscStartAsc().stream().map(SeatingDto::from).toList();
    }

    @Transactional
    public SeatingDto create(SaveSeatingRequest req) {
        validate(req);
        Seating s = new Seating();
        apply(s, req);
        return SeatingDto.from(repo.save(s));
    }

    @Transactional
    public SeatingDto update(String id, SaveSeatingRequest req) {
        validate(req);
        Seating s = findOrThrow(id);
        apply(s, req);
        return SeatingDto.from(repo.save(s));
    }

    @Transactional
    public void toggleActive(String id) {
        Seating s = findOrThrow(id);
        s.setActive(!s.isActive());
        repo.save(s);
    }

    @Transactional
    public void delete(String id) {
        if (reservationRepo.existsBySeating_Id(id)) {
            throw AppException.conflict("El turno tiene reservas; desactívalo en vez de borrarlo.");
        }
        repo.delete(findOrThrow(id));
    }

    public Seating findOrThrow(String id) {
        return repo.findById(id).orElseThrow(() -> AppException.notFound("Turno no encontrado: " + id));
    }

    private void validate(SaveSeatingRequest req) {
        if (!LocalTime.parse(req.start()).isBefore(LocalTime.parse(req.end()))) {
            throw AppException.badRequest("La hora de fin del turno debe ser posterior al inicio.");
        }
    }

    private void apply(Seating s, SaveSeatingRequest req) {
        s.setName(req.name());
        s.setStart(req.start());
        s.setEnd(req.end());
        s.setDurationMinutes(req.durationMinutes());
        s.setDaysOfWeek(new LinkedHashSet<>(req.daysOfWeek()));
        s.setSortOrder(req.sortOrder());
    }
}
