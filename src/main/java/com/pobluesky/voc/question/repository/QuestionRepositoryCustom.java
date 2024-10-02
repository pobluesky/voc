package com.pobluesky.voc.question.repository;

import com.pobluesky.voc.question.dto.response.QuestionSummaryResponseDTO;
import com.pobluesky.voc.question.entity.QuestionStatus;
import com.pobluesky.voc.question.entity.QuestionType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionRepositoryCustom {
    Page<QuestionSummaryResponseDTO> findQuestionsByManager(
        Pageable pageable,
        QuestionStatus status,
        QuestionType type,
        String title,
        Long questionId,
        String customerName,
        Boolean isActivated,
        LocalDate startDate,
        LocalDate endDate,
        Long managerId,
        String sortBy
    );

    Page<QuestionSummaryResponseDTO> findQuestionsByCustomer(
        Pageable pageable,
        Long userId,
        QuestionStatus status,
        QuestionType type,
        String title,
        Long questionId,
        LocalDate startDate,
        LocalDate endDate,
        String sortBy
    );

    List<QuestionSummaryResponseDTO> findQuestionsBySearch(
            String sortBy,
            QuestionStatus status,
            QuestionType type,
            String title,
            String customerName,
            LocalDate startDate,
            LocalDate endDate
    );
}
