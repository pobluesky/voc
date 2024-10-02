package com.pobluesky.voc.question.entity;

import lombok.Getter;

@Getter
public enum QuestionStatus {

    READY("답변대기"),
    COMPLETED("답변완료");

    private String status;

    QuestionStatus(String status) {
        this.status = status;
    }

    public static QuestionStatus fromString(String status) {
        for (QuestionStatus questionStatus : QuestionStatus.values()) {
            if (questionStatus.getStatus().equals(status)) {
                return questionStatus;
            }
        }

        throw new IllegalArgumentException("Unknown status: " + status);
    }
}
