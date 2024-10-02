package com.pobluesky.voc.question.dto.response;

import com.pobluesky.voc.feign.Customer;
import com.pobluesky.voc.feign.UserClient;
import com.pobluesky.voc.question.entity.Question;
import com.pobluesky.voc.question.entity.QuestionStatus;
import com.pobluesky.voc.question.entity.QuestionType;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record QuestionSummaryResponseDTO(

    Long questionId,

    String title,

    QuestionStatus status,

    QuestionType type,

    String contents,

    String customerName,

    LocalDateTime questionCreatedAt,

    LocalDateTime answerCreatedAt,

    Long managerId,

    Boolean isActivated
) {
    public static QuestionSummaryResponseDTO from(Question question, UserClient userClient) {
        Customer customer = userClient.getCustomerByIdWithoutToken(question.getUserId()).getData();

        return QuestionSummaryResponseDTO.builder()
            .questionId(question.getQuestionId())
            .title(question.getTitle())
            .status(question.getStatus())
            .type(question.getType())
            .contents(question.getContents())
            .customerName(customer.getName())
            .questionCreatedAt(question.getCreatedDate())
            .answerCreatedAt(question.getAnswer() != null ? question.getAnswer().getCreatedDate() : null)
            .managerId(question.getAnswer() != null ? question.getAnswer().getManagerId(): null)
            .isActivated(question.getIsActivated())
            .build();
    }
}
