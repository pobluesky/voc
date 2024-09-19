package com.pobluesky.voc.answer.dto.request;


import com.pobluesky.voc.answer.entity.Answer;
import com.pobluesky.voc.feign.Customer;
import com.pobluesky.voc.feign.Inquiry;
import com.pobluesky.voc.feign.Manager;
import com.pobluesky.voc.question.entity.Question;

public record AnswerCreateRequestDTO(
    String title,
    String contents
) {
    public Answer toAnswerEntity(Question question, Long inquiryId, Long customerId, Long managerId, String fileName, String filePath) {
        return Answer.builder()
            .question(question)
            .inquiryId(inquiryId)
            .customerId(customerId)
            .managerId(managerId)
            .title(title)
            .contents(contents)
            .fileName(fileName)
            .filePath(filePath)
            .build();
    }
}
