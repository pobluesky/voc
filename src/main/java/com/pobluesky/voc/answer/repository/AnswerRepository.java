package com.pobluesky.voc.answer.repository;

import com.pobluesky.voc.answer.entity.Answer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findAllByCustomerId(Long userId);

    Optional<Answer> findByQuestion_QuestionId(Long questionId);
}
