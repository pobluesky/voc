package com.pobluesky.voc.collaboration.dto.response;

import com.pobluesky.voc.feign.Manager;
import com.pobluesky.voc.global.entity.Department;
import lombok.Builder;

@Builder
public record ManagerResponseDTO(

    Long userId,

    String empNo,

    String name,

    Department department
) {

    public static ManagerResponseDTO from(Manager manager) {

        return ManagerResponseDTO.builder()
            .userId(manager.getUserId())
            .empNo(manager.getEmpNo())
            .name(manager.getName())
            .department(manager.getDepartment())
            .build();
    }
}
