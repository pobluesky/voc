package com.pobluesky.voc.question.dto.request;

import com.pobluesky.voc.question.entity.Question;
import com.pobluesky.voc.question.entity.QuestionStatus;
import com.pobluesky.voc.question.entity.QuestionType;

public record QuestionCreateRequestDTO(

    String title,

    String contents,

    QuestionStatus status,

    QuestionType type
) {
    public Question toQuestionEntity(Long inquiryId, Long customerId, String fileName, String filePath) {

        return Question.builder()
            .userId(customerId)
            .inquiryId(inquiryId)
            .title(title)
            .contents(contents)
            .fileName(fileName)
            .filePath(filePath)
            .status(status)
            .type(type)
            .build();
    }
}
