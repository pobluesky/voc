package com.pobluesky.voc.answer.dto.response;

import com.pobluesky.voc.answer.entity.Answer;
import com.pobluesky.voc.feign.Inquiry;
import com.pobluesky.voc.feign.InquiryClient;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Builder
public record AnswerResponseDTO(

    Long questionId,

    Optional<Long> inquiryId,

    Long customerId,

    Long managerId,

    String title,

    String contents,

    String fileName,

    String filePath,

    LocalDateTime createdDate,

    Boolean isActivated
) {

    public static AnswerResponseDTO from(Answer answer, InquiryClient inquiryClient) {
        Optional<Long> inquiryId = Optional.ofNullable(
            answer.getInquiryId() != null
                ? inquiryClient.getInquiryByIdWithoutToken(answer.getInquiryId()).getData().getInquiryId()
                : null
        );

        return AnswerResponseDTO.builder()
            .inquiryId(inquiryId)
            .questionId(answer.getQuestion().getQuestionId())
            .customerId(answer.getCustomerId())
            .managerId(answer.getManagerId())
            .title(answer.getTitle())
            .contents(answer.getContents())
            .createdDate(answer.getCreatedDate())
            .fileName(answer.getFileName())
            .filePath(answer.getFilePath())
            .isActivated(answer.getIsActivated())
            .build();
    }
}
