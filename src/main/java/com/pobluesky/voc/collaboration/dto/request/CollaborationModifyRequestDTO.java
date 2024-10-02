package com.pobluesky.voc.collaboration.dto.request;

public record CollaborationModifyRequestDTO(

    Long colReqId,

    Long colResId,

    String colContents,

    Boolean isAccepted,

    String colReply,

    Boolean isFileDeleted
) {
}
