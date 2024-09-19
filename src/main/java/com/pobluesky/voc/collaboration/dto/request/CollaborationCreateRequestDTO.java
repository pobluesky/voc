package com.pobluesky.voc.collaboration.dto.request;

import com.pobluesky.voc.collaboration.entity.Collaboration;
import com.pobluesky.voc.feign.Manager;
import com.pobluesky.voc.question.entity.Question;

public record CollaborationCreateRequestDTO(
    Long colReqId,
    Long colResId,
    String colContents
) {

    public Collaboration toCollaborationEntity(
        Long colRequestManagerId,
        Long colResponseManagerId,
        Question question,
        String fileName,
        String filePath
    ) {

        return Collaboration.builder()
            .question(question)
            .colRequestId(colRequestManagerId)
            .colResponseId(colResponseManagerId)
            .colContents(colContents)
            .fileName(fileName)
            .filePath(filePath)
            .build();
    }
}
