package dev.pimon.newsletter.repository;

import dev.pimon.newsletter.entity.NewsletterCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterCampaignRepository extends JpaRepository<NewsletterCampaign, String> {

    Page<NewsletterCampaign> findAllByOrderBySentAtDesc(Pageable pageable);
}
