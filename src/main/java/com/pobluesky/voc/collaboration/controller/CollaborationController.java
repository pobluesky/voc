package com.pobluesky.voc.collaboration.controller;

import com.pobluesky.voc.collaboration.dto.request.CollaborationCreateRequestDTO;
import com.pobluesky.voc.collaboration.dto.request.CollaborationModifyRequestDTO;
import com.pobluesky.voc.collaboration.dto.request.CollaborationUpdateRequestDTO;
import com.pobluesky.voc.collaboration.dto.response.CollaborationDetailResponseDTO;
import com.pobluesky.voc.collaboration.dto.response.CollaborationResponseDTO;
import com.pobluesky.voc.collaboration.dto.response.CollaborationSummaryResponseDTO;
import com.pobluesky.voc.collaboration.entity.ColStatus;
import com.pobluesky.voc.collaboration.service.CollaborationService;
import com.pobluesky.voc.global.util.ResponseFactory;
import com.pobluesky.voc.global.util.model.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collaborations")
public class CollaborationController {
    private final CollaborationService collaborationService;

    @GetMapping
    @Operation(summary = "협업 목록 조회", description = "협업 요청을 받은 담당자의 협업 목록을 전부 조회한다.")
    public ResponseEntity<JsonResult> getAllCollaborations(
        @RequestHeader("Authorization") String token,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "15") int size,
        @RequestParam(defaultValue = "LATEST") String sortBy,
        @RequestParam(required = false) Long colId,
        @RequestParam(required = false) ColStatus colStatus,
        @RequestParam(required = false) String colReqManager,
        @RequestParam(required = false) Long colReqId,
        @RequestParam(required = false) String colResManager,
        @RequestParam(required = false) Long colResId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        Page<CollaborationSummaryResponseDTO> cols = collaborationService.getAllCollaborations(
            token,
            page,
            size,
            sortBy,
            colId,
            colStatus,
            colReqManager,
            colReqId,
            colResManager,
            colResId,
            startDate,
            endDate
        );

        Map<String, Object> response = new HashMap<>();

        response.put("colListInfo", cols.getContent());
        response.put("totalElements", cols.getTotalElements());
        response.put("totalPages", cols.getTotalPages());

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @Operation(summary = "questionId 별 협업 요청")
    @PostMapping("/{questionId}")
    public ResponseEntity<JsonResult> createCollaboration(
        @RequestHeader("Authorization") String token,
        @PathVariable Long questionId,
        @RequestPart(value = "files", required = false) MultipartFile file,
        @RequestPart("collaboration") CollaborationCreateRequestDTO requestDTO
    ) {
        CollaborationResponseDTO response = collaborationService.createCollaboration(
            token,
            questionId,
            file,
            requestDTO
        );

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @GetMapping("/{questionId}/{collaborationId}")
    @Operation(summary = "협업 단건 조회")
    public ResponseEntity<JsonResult> getCollaboration(
        @RequestHeader("Authorization") String token,
        @PathVariable Long questionId,
        @PathVariable Long collaborationId
    ) {
        CollaborationDetailResponseDTO response = collaborationService.getCollaborationById(
            token,
            questionId,
            collaborationId
        );

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @Operation(
        summary = "collaborationId 별 협업 수락/거절 결정",
        description = "협업 요청을 받은 담당자만 수정 가능"
    )
    @PutMapping("/{collaborationId}/decision")
    public ResponseEntity<JsonResult> updateCollaborationStatus(
        @RequestHeader("Authorization") String token,
        @PathVariable Long collaborationId,
        @RequestPart(value = "files", required = false) MultipartFile file,
        @RequestPart("collaboration") CollaborationUpdateRequestDTO requestDTO
    ) {
        CollaborationDetailResponseDTO response = collaborationService.updateCollaborationStatus(
            token,
            collaborationId,
            file,
            requestDTO
            );

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @Operation(
        summary = "collaborationId 별 협업 완료 결정",
        description = "협업 요청을 받은 담당자만 수정 가능"
    )
    @PutMapping("/{collaborationId}/decision/complete")
    public ResponseEntity<JsonResult> updateCollaborationStatus(
        @RequestHeader("Authorization") String token,
        @PathVariable Long collaborationId
    ) {
        CollaborationDetailResponseDTO response = collaborationService.completeCollaboration(
            token,
            collaborationId
        );

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @Operation(
        summary = "collaborationId 별 협업 수락/거절",
        description = "담당자별 협업 요청건에 대한 수락/거절 상태 수정"
    )
    @PutMapping("/{collaborationId}/modify")
    public ResponseEntity<JsonResult> modifyCollaboration(
        @RequestHeader("Authorization") String token,
        @PathVariable Long collaborationId,
        @RequestPart(value = "files", required = false) MultipartFile file,
        @RequestPart("collaboration") CollaborationModifyRequestDTO requestDTO
    ) {
        CollaborationDetailResponseDTO response = collaborationService.modifyCollaboration(
            token,
            collaborationId,
            file,
            requestDTO
        );

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    /* [Start] Dashboard API */
    @GetMapping("/managers/col/dashboard")
    @Operation(summary = "월별 협업 처리 건수 평균")
    public ResponseEntity<Map<String, List<Object[]>>> averageMonthlyCol(
        @RequestHeader("Authorization") String token
    ) {
        Map<String, List<Object[]>> response = collaborationService.getAverageCountPerMonth(token);

        return ResponseEntity.ok(response);
    }
    /* [End] Dashboard API */
}
