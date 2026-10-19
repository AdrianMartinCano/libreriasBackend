package dev.pimon.events.repository;

import dev.pimon.events.entity.Rsvp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RsvpRepository extends JpaRepository<Rsvp, String> {

    List<Rsvp> findByEventIdOrderByCreatedAtDesc(String eventId);
}
