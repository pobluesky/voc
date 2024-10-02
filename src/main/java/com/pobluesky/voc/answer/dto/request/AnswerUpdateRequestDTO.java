package com.pobluesky.voc.answer.dto.request;

public record AnswerUpdateRequestDTO (

    String title,

    String contents,

    String fileName,

    String filePath,

    Boolean isFileDeleted
) {
}
