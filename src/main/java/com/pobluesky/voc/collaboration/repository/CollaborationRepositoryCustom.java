package com.pobluesky.voc.collaboration.repository;

import com.pobluesky.voc.collaboration.dto.response.CollaborationSummaryResponseDTO;
import com.pobluesky.voc.collaboration.entity.ColStatus;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CollaborationRepositoryCustom {
    Page<CollaborationSummaryResponseDTO> findAllCollaborationsRequest(
        Pageable pageable,
        Long colId,
        ColStatus colStatus,
        String colReqManager,
        Long colReqId,
        String colResManager,
        Long colResId,
        LocalDate startDate,
        LocalDate endDate,
        String sortBy
    );
}
