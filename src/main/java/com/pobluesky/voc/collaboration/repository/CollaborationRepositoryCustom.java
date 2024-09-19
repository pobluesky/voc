package com.pobluesky.voc.collaboration.repository;

import com.pobluesky.voc.collaboration.dto.response.CollaborationSummaryResponseDTO;
import com.pobluesky.voc.collaboration.entity.ColStatus;
import java.time.LocalDate;
import java.util.List;

public interface CollaborationRepositoryCustom {
    List<CollaborationSummaryResponseDTO> findAllCollaborationsRequestWithoutPaging(
        ColStatus colStatus,
        String colReqManager,
        Long colReqId,
        LocalDate startDate,
        LocalDate endDate,
        String sortBy
    );
}
