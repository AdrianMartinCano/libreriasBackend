package dev.pimon.newsletter.web;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.newsletter.dto.CampaignDto;
import dev.pimon.newsletter.dto.CampaignPreviewDto;
import dev.pimon.newsletter.dto.CampaignPreviewRequest;
import dev.pimon.newsletter.dto.CampaignTemplateDto;
import dev.pimon.newsletter.dto.SendCampaignRequest;
import dev.pimon.newsletter.dto.SubscriberDto;
import dev.pimon.newsletter.service.NewsletterSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/newsletter")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
@RequiredArgsConstructor
public class AdminNewsletterController {

    private final NewsletterSubscriptionService service;

    @GetMapping("/subscribers")
    public ApiResponse<PageResponse<SubscriberDto>> listSubscribers(
            @RequestParam(required = false) String status,
            @ParameterObject Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(service.list(status, pageable)));
    }

    @GetMapping(value = "/subscribers/export", produces = "text/csv")
    public ResponseEntity<String> exportSubscribers(@RequestParam(required = false) String status) {
        String csv = service.exportCsv(status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"newsletter-subscribers.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @PostMapping("/campaigns")
    public ApiResponse<CampaignDto> sendCampaign(@Valid @RequestBody SendCampaignRequest req) {
        return ApiResponse.ok(service.sendCampaign(req), "Campaña enviada correctamente");
    }

    @GetMapping("/campaigns")
    public ApiResponse<PageResponse<CampaignDto>> listCampaigns(@ParameterObject Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(service.listCampaigns(pageable)));
    }

    @GetMapping("/campaign-templates")
    public ApiResponse<List<CampaignTemplateDto>> listCampaignTemplates() {
        return ApiResponse.ok(service.listCampaignTemplates());
    }

    @PostMapping("/campaigns/preview")
    public ApiResponse<CampaignPreviewDto> previewCampaign(@RequestBody CampaignPreviewRequest req) {
        return ApiResponse.ok(service.renderPreview(req));
    }
}
