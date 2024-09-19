package com.pobluesky.voc.feign;

import com.pobluesky.voc.global.entity.Department;
import com.pobluesky.voc.global.security.UserRole;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Manager {
    Long userId;
    String empNo;
    String name;
    Department department;
    UserRole role;

}
