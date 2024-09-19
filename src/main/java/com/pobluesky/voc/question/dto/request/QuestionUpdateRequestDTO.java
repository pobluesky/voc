package com.pobluesky.voc.question.dto.request;

import com.pobluesky.voc.feign.Inquiry;
import com.pobluesky.voc.question.entity.QuestionStatus;
import com.pobluesky.voc.question.entity.QuestionType;

public record QuestionUpdateRequestDTO(

    Inquiry inquiry,

    String title,

    String contents,

    QuestionStatus status,

    QuestionType type
) {
}
