package com.pobluesky.voc.collaboration.dto.request;

public record CollaborationUpdateRequestDTO(
    Long colReqId,
    Long colResId,
    String colReply,
    Boolean isAccepted
) {

}
