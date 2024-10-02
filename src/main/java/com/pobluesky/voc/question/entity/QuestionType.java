package com.pobluesky.voc.question.entity;

import lombok.Getter;

@Getter
public enum QuestionType {

    INQ("주문문의"),
    SITE("사이트이용문의"),
    ETC("기타문의");

    private final String type;

    QuestionType(String type) {
        this.type = type;
    }

    public static QuestionType fromString(String type) {
        for (QuestionType questionType : QuestionType.values()) {
            if (questionType.getType().equals(type))
                return questionType;
        }

        throw new IllegalArgumentException("Unknown type: " + type);
    }
}
