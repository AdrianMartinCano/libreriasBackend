package dev.pimon.blog.repository;

import dev.pimon.blog.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(String postId);
    long countByPostId(String postId);
}
