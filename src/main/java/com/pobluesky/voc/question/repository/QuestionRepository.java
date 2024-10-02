package com.pobluesky.voc.question.repository;

import com.pobluesky.voc.question.entity.Question;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>, QuestionRepositoryCustom {

    @Query("SELECT q FROM Question q WHERE q.questionId = :questionId AND q.isActivated = true")
    Optional<Question> findActiveQuestionByQuestionId(Long questionId);

    @Query("SELECT q FROM Question q WHERE q.isActivated = true")
    List<Question> findActiveQuestions();
}
