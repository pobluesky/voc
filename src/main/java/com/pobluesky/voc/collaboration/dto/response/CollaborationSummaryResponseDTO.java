package com.pobluesky.voc.collaboration.dto.response;

import com.pobluesky.voc.collaboration.entity.ColStatus;
import com.pobluesky.voc.collaboration.entity.Collaboration;
import com.pobluesky.voc.feign.Manager;
import com.pobluesky.voc.feign.UserClient;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CollaborationSummaryResponseDTO(
    Long colId,
    Long questionId,
    Long colReqId,
    String colReqManager,
    Long colResId,
    String colResManager,
    ColStatus colStatus,
    String colContents,
    LocalDateTime createdDate
) {

    public static CollaborationSummaryResponseDTO from(Collaboration collaboration, UserClient userClient) {
        Manager reqmanager = userClient.getManagerByIdWithoutToken(
            collaboration.getColRequestId()).getData();

        Manager resmanager = userClient.getManagerByIdWithoutToken(
            collaboration.getColResponseId()).getData();

        return CollaborationSummaryResponseDTO.builder()
            .colId(collaboration.getColId())
            .questionId(collaboration.getQuestion().getQuestionId())
            .colReqId(reqmanager.getUserId())
            .colReqManager(reqmanager.getName())
            .colResId(resmanager.getUserId())
            .colResManager(reqmanager.getName())
            .colStatus(collaboration.getColStatus())
            .colContents(collaboration.getColContents())
            .createdDate(collaboration.getCreatedDate())
            .build();
    }
}
