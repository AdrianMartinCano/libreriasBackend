package dev.pimon.forum.service;

import dev.pimon.common.dto.PageResponse;
import dev.pimon.common.exception.AppException;
import dev.pimon.forum.dto.CreateReportRequest;
import dev.pimon.forum.dto.ReportDto;
import dev.pimon.forum.entity.ForumPost;
import dev.pimon.forum.entity.Report;
import dev.pimon.forum.entity.ReportStatus;
import dev.pimon.forum.repository.ForumPostRepository;
import dev.pimon.forum.repository.ReportRepository;
import dev.pimon.security.model.AuthUser;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Denuncias de mensajes: el usuario reporta, el admin revisa y resuelve.
 */
@Service
@Transactional
public class ReportService {

    private static final int PREVIEW_LEN = 140;

    private final ReportRepository    reportRepo;
    private final ForumPostRepository postRepo;
    private final ForumNames          names;

    public ReportService(ReportRepository reportRepo, ForumPostRepository postRepo, ForumNames names) {
        this.reportRepo = reportRepo;
        this.postRepo   = postRepo;
        this.names      = names;
    }

    public void create(String postId, CreateReportRequest req, AuthUser user) {
        if (user == null || user.id() == null) {
            throw AppException.unauthorized("Debes iniciar sesión para denunciar");
        }
        if (!postRepo.existsById(postId)) {
            throw AppException.notFound("Mensaje no encontrado: " + postId);
        }
        Report r = new Report();
        r.setPostId(postId);
        r.setReporterId(user.id());
        r.setReporterName(names.forWriting(user));
        r.setReason(req.reason());
        r.setStatus(ReportStatus.OPEN);
        reportRepo.save(r);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportDto> list(boolean onlyOpen, Pageable pageable) {
        var page = onlyOpen
                ? reportRepo.findByStatusOrderByCreatedAtDesc(ReportStatus.OPEN, pageable)
                : reportRepo.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(page.map(this::toDto));
    }

    public void resolve(String id) {
        Report r = reportRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Denuncia no encontrada: " + id));
        r.setStatus(ReportStatus.RESOLVED);
        reportRepo.save(r);
    }

    void deleteForPost(String postId) {
        reportRepo.deleteByPostId(postId);
    }

    private ReportDto toDto(Report r) {
        ForumPost post = postRepo.findById(r.getPostId()).orElse(null);
        String threadId = post != null ? post.getThreadId() : null;
        String preview = post != null ? preview(post.getBody()) : "(mensaje eliminado)";
        return new ReportDto(r.getId(), r.getPostId(), threadId, null,
                r.getReporterName(), r.getReason(), r.getStatus().name(), preview, r.getCreatedAt());
    }

    private static String preview(String body) {
        if (body == null) return "";
        String t = body.strip();
        return t.length() <= PREVIEW_LEN ? t : t.substring(0, PREVIEW_LEN) + "…";
    }
}
