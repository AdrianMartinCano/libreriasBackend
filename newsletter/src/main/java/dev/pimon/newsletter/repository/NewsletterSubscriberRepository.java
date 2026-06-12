package dev.pimon.newsletter.repository;

import dev.pimon.newsletter.entity.NewsletterSubscriber;
import dev.pimon.newsletter.entity.NewsletterSubscriber.SubscriberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, String> {

    Optional<NewsletterSubscriber> findByEmail(String email);

    Optional<NewsletterSubscriber> findByConfirmationToken(String token);

    Optional<NewsletterSubscriber> findByUnsubscribeToken(String token);

    Page<NewsletterSubscriber> findByStatus(SubscriberStatus status, Pageable pageable);

    long countByStatus(SubscriberStatus status);
}
