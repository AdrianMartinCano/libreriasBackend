package dev.pimon.reservations.service;

import lombok.RequiredArgsConstructor;
import dev.pimon.common.exception.AppException;
import dev.pimon.reservations.dto.CreateServiceOfferingRequest;
import dev.pimon.reservations.dto.ServiceOfferingDto;
import dev.pimon.reservations.entity.ServiceOffering;
import dev.pimon.reservations.repository.ServiceOfferingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceOfferingService {

    private final ServiceOfferingRepository repo;

    public List<ServiceOfferingDto> listActive() {
        return repo.findByActiveTrueOrderByName().stream()
                .map(ServiceOfferingDto::from)
                .toList();
    }

    public List<ServiceOfferingDto> listAll() {
        return repo.findAll().stream()
                .map(ServiceOfferingDto::from)
                .toList();
    }

    @Transactional
    public ServiceOfferingDto create(CreateServiceOfferingRequest req) {
        ServiceOffering s = new ServiceOffering(
                req.name(), req.description(), req.durationMinutes(),
                req.price(), req.imageUrl(), req.color()
        );
        return ServiceOfferingDto.from(repo.save(s));
    }

    @Transactional
    public ServiceOfferingDto update(String id, CreateServiceOfferingRequest req) {
        ServiceOffering s = findOrThrow(id);
        s.setName(req.name());
        s.setDescription(req.description());
        s.setDurationMinutes(req.durationMinutes());
        s.setPrice(req.price());
        s.setImageUrl(req.imageUrl());
        s.setColor(req.color());
        return ServiceOfferingDto.from(repo.save(s));
    }

    @Transactional
    public void toggleActive(String id) {
        ServiceOffering s = findOrThrow(id);
        s.setActive(!s.isActive());
        repo.save(s);
    }

    public ServiceOffering findOrThrow(String id) {
        return repo.findById(id)
                .orElseThrow(() -> AppException.notFound("Servicio no encontrado con id: " + id));
    }
}
