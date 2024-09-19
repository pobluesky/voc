package com.pobluesky.voc.answer.dto.response;

import com.pobluesky.voc.answer.entity.Answer;
import lombok.Builder;

@Builder
public record MobileAnswerSummaryResponseDTO(

        String title,

        String contents
) {
    public static MobileAnswerSummaryResponseDTO from(Answer answer) {

        return MobileAnswerSummaryResponseDTO.builder()
                .title(answer.getTitle())
                .contents(answer.getContents())
                .build();
    }
}
