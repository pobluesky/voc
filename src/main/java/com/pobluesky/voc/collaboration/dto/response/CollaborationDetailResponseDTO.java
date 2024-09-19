package com.pobluesky.voc.collaboration.dto.response;

import com.pobluesky.voc.collaboration.entity.ColStatus;
import com.pobluesky.voc.collaboration.entity.Collaboration;
import com.pobluesky.voc.feign.Manager;
import com.pobluesky.voc.feign.UserClient;
import lombok.Builder;

@Builder
public record CollaborationDetailResponseDTO(
    Long colId,
    Long questionId,
    ManagerResponseDTO colManagerFromResponseDto,
    ManagerResponseDTO colManagerToResponseDto,
    ColStatus colStatus,
    String colContents,
    String colReply,
    String fileName,
    String filePath,
    String vocFileName,
    String vocFilePath
) {

    public static CollaborationDetailResponseDTO from(Collaboration collaboration, UserClient userClient) {
        Manager salesManager = userClient.getManagerByIdWithoutToken(
            collaboration.getColRequestId()).getData();
        Manager qualityManager = userClient.getManagerByIdWithoutToken(
            collaboration.getColResponseId()).getData();

        return CollaborationDetailResponseDTO.builder()
            .colId(collaboration.getColId())
            .questionId(collaboration.getQuestion().getQuestionId())
            .colManagerFromResponseDto(ManagerResponseDTO.from(salesManager))
            .colManagerToResponseDto(ManagerResponseDTO.from(qualityManager))
            .colStatus(collaboration.getColStatus())
            .colContents(collaboration.getColContents())
            .colReply(collaboration.getColReply())
            .fileName(collaboration.getFileName())
            .filePath(collaboration.getFilePath())
            .vocFileName(collaboration.getQuestion().getFileName())
            .vocFilePath(collaboration.getQuestion().getFilePath())
            .build();
    }
}
