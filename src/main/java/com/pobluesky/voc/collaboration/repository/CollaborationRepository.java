package com.pobluesky.voc.collaboration.repository;

import com.pobluesky.voc.collaboration.entity.Collaboration;
import com.pobluesky.voc.question.entity.Question;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CollaborationRepository extends JpaRepository<Collaboration, Long>, CollaborationRepositoryCustom {

    @Query("SELECT c FROM Collaboration c WHERE c.colId = :collaborationId AND c.question = :question")
    Optional<Collaboration> findByIdAndQuestion(@Param("collaborationId") Long collaborationId, @Param("question") Question question);
}
