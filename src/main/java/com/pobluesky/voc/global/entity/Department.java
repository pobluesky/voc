package com.pobluesky.voc.global.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Department {

    HR("HR", "Human Resources"),
    IT("IT", "Information Technology"),
    SALES("SALES", "Sales Department"),
    FINANCE("FINANCE", "Finance Department");

    private final String code;

    private final String name;
}
