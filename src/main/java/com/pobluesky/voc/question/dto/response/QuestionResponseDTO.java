package com.pobluesky.voc.question.dto.response;

import com.pobluesky.voc.collaboration.entity.ColStatus;
import com.pobluesky.voc.feign.Customer;
import com.pobluesky.voc.feign.Inquiry;
import com.pobluesky.voc.feign.InquiryClient;
import com.pobluesky.voc.feign.UserClient;
import com.pobluesky.voc.question.entity.Question;
import com.pobluesky.voc.question.entity.QuestionStatus;
import com.pobluesky.voc.question.entity.QuestionType;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.Builder;

@Builder
public record QuestionResponseDTO(

    Optional<Long> inquiryId,

    Long userId,

    Long questionId,

    String customerName,

    String title,

    String contents,

    String fileName,

    String filePath,

    QuestionStatus status,

    QuestionType type,

    LocalDateTime createdDate,

    Long colId,

    ColStatus colStatus,

    Boolean isActivated
) {
    public static QuestionResponseDTO from(Question question, UserClient userClient,
        InquiryClient inquiryClient) {
        Customer customer = userClient.getCustomerByIdWithoutToken(question.getUserId()).getData();

        Optional<Long> inquiryId = Optional.empty();
        if (question.getInquiryId() != null) {
            Inquiry inquiry = inquiryClient.getInquiryByIdWithoutToken(question.getInquiryId()).getData();
            inquiryId = Optional.ofNullable(inquiry.getInquiryId());
        }

        return QuestionResponseDTO.builder()
            .inquiryId(inquiryId)
            .userId(question.getUserId())
            .questionId(question.getQuestionId())
            .customerName(customer.getName())
            .title(question.getTitle())
            .contents(question.getContents())
            .fileName(question.getFileName())
            .filePath(question.getFilePath())
            .status(question.getStatus())
            .type(question.getType())
            .createdDate(question.getCreatedDate())
            .colId(question.getCollaboration() != null ? question.getCollaboration().getColId() : null)
            .colStatus(question.getCollaboration() != null ? question.getCollaboration().getColStatus() : null)
            .isActivated(question.getIsActivated())
            .build();
    }
}
