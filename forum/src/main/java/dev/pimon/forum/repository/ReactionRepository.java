package dev.pimon.forum.repository;

import dev.pimon.forum.entity.Reaction;
import dev.pimon.forum.entity.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, String> {

    List<Reaction> findByPostIdIn(List<String> postIds);

    List<Reaction> findByPostId(String postId);

    Optional<Reaction> findByPostIdAndUserIdAndType(String postId, String userId, ReactionType type);

    void deleteByPostId(String postId);
}
