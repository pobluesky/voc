package com.pobluesky.voc.collaboration.entity;

import lombok.Getter;

@Getter
public enum ColStatus {

    READY("ready"),
    INPROGRESS("inprogress"),
    REFUSE("refuse"),
    COMPLETE("complete");

    private String status;

    ColStatus(String status) {
        this.status = status;
    }
}
