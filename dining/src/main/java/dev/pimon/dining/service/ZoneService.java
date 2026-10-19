package dev.pimon.dining.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.dining.dto.SaveZoneRequest;
import dev.pimon.dining.dto.ZoneDto;
import dev.pimon.dining.entity.Zone;
import dev.pimon.dining.repository.DiningTableRepository;
import dev.pimon.dining.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository        repo;
    private final DiningTableRepository tableRepo;

    public List<ZoneDto> listActive() {
        return repo.findByActiveTrueOrderBySortOrderAscNameAsc().stream().map(ZoneDto::from).toList();
    }

    public List<ZoneDto> listAll() {
        return repo.findAllByOrderBySortOrderAscNameAsc().stream().map(ZoneDto::from).toList();
    }

    @Transactional
    public ZoneDto create(SaveZoneRequest req) {
        return ZoneDto.from(repo.save(new Zone(req.name(), req.sortOrder())));
    }

    @Transactional
    public ZoneDto update(String id, SaveZoneRequest req) {
        Zone z = findOrThrow(id);
        z.setName(req.name());
        z.setSortOrder(req.sortOrder());
        return ZoneDto.from(repo.save(z));
    }

    @Transactional
    public void toggleActive(String id) {
        Zone z = findOrThrow(id);
        z.setActive(!z.isActive());
        repo.save(z);
    }

    @Transactional
    public void delete(String id) {
        if (tableRepo.existsByZone_Id(id)) {
            throw AppException.conflict("La zona tiene mesas; muévelas o elimínalas antes (o desactívala).");
        }
        repo.delete(findOrThrow(id));
    }

    public Zone findOrThrow(String id) {
        return repo.findById(id).orElseThrow(() -> AppException.notFound("Zona no encontrada: " + id));
    }
}
