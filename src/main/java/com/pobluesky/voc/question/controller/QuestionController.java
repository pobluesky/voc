package com.pobluesky.voc.question.controller;

import com.pobluesky.voc.global.util.ResponseFactory;
import com.pobluesky.voc.global.util.model.CommonResult;
import com.pobluesky.voc.global.util.model.JsonResult;
import com.pobluesky.voc.question.dto.request.QuestionCreateRequestDTO;
import com.pobluesky.voc.question.dto.request.QuestionUpdateRequestDTO;
import com.pobluesky.voc.question.dto.response.QuestionResponseDTO;
import com.pobluesky.voc.question.dto.response.QuestionSummaryResponseDTO;
import com.pobluesky.voc.question.entity.QuestionStatus;
import com.pobluesky.voc.question.entity.QuestionType;
import com.pobluesky.voc.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/managers")
    @Operation(summary = "질문 조회(담당자)", description = "등록된 모든 질문을 조건에 맞게 조회한다.")
    public ResponseEntity<JsonResult> getQuestionByManager(
        @RequestHeader("Authorization") String token,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "15") int size,
        @RequestParam(defaultValue = "LATEST") String sortBy,
        @RequestParam(required = false) QuestionStatus status,
        @RequestParam(required = false) QuestionType type,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) Long questionId,
        @RequestParam(required = false) String customerName,
        @RequestParam(required = false) Boolean isActivated,
        @RequestParam(required = false) Long managerId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        Page<QuestionSummaryResponseDTO> questions = questionService.getQuestionsByManager(
            token,
            page,
            size,
            sortBy,
            status,
            type,
            title,
            questionId,
            customerName,
            isActivated,
            managerId,
            startDate,
            endDate
        );

        Map<String, Object> response = new HashMap<>();

        response.put("questionsInfo", questions.getContent());
        response.put("totalElements", questions.getTotalElements());
        response.put("totalPages", questions.getTotalPages());

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @GetMapping("/managers/{questionId}")
    @Operation(summary = "질문별 상세 조회(담당자)", description = "등록된 질문을 질문 번호로 조회한다.")
    public ResponseEntity<JsonResult> getQuestionByQuestionIdForManager(
        @RequestHeader("Authorization") String token,
        @PathVariable Long questionId
    ) {
        QuestionResponseDTO response = questionService.getQuestionByQuestionIdForManager(
            token,
            questionId
        );

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @GetMapping("/customers/{userId}")
    @Operation(summary = "질문 조회(고객사)", description = "등록된 모든 질문을 조건에 맞게 조회한다.")
    public ResponseEntity<JsonResult> getQuestionsByCustomer(
        @RequestHeader("Authorization") String token,
        @PathVariable Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "15") int size,
        @RequestParam(defaultValue = "LATEST") String sortBy,
        @RequestParam(required = false) QuestionStatus status,
        @RequestParam(required = false) QuestionType type,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) Long questionId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        Page<QuestionSummaryResponseDTO> questions = questionService.getQuestionsByCustomer(
            token,
            userId,
            page,
            size,
            sortBy,
            status,
            type,
            title,
            questionId,
            startDate,
            endDate
        );

        Map<String, Object> response = new HashMap<>();

        response.put("questionsInfo", questions.getContent());
        response.put("totalElements", questions.getTotalElements());
        response.put("totalPages", questions.getTotalPages());

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @GetMapping("/customers/{userId}/{questionId}")
    @Operation(summary = "질문별 상세 조회(고객사)", description = "등록된 질문을 질문 번호로 조회한다.")
    public ResponseEntity<JsonResult> getQuestionByQuestionId(
        @RequestHeader("Authorization") String token,
        @PathVariable Long userId,
        @PathVariable Long questionId
    ) {
        QuestionResponseDTO response = questionService.getQuestionByQuestionId(
            token,
            userId,
            questionId
        );

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @PostMapping("/customers/{userId}/{inquiryId}")
    @Operation(summary = "문의별 질문 작성(고객사)", description = "특정 문의에 대한 새로운 질문을 등록한다.")
    public ResponseEntity<JsonResult> createQuestion(
        @RequestHeader("Authorization") String token,
        @PathVariable Long userId,
        @PathVariable Long inquiryId,
        @RequestPart(value = "files", required = false) MultipartFile file,
        @RequestPart("question") QuestionCreateRequestDTO questionCreateRequestDTO) {
        QuestionResponseDTO response = questionService.createInquiryQuestion(
            token,
            userId,
            inquiryId,
            file,
            questionCreateRequestDTO
        );

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @PostMapping("/customers/{userId}")
    @Operation(summary = "기타 질문 작성(고객사)", description = "문의 외적인 새로운 질문을 등록한다.")
    public ResponseEntity<JsonResult> createQuestion(
        @RequestHeader("Authorization") String token,
        @PathVariable Long userId,
        @RequestPart(value = "files", required = false) MultipartFile file,
        @RequestPart("question") QuestionCreateRequestDTO questionCreateRequestDTO) {
        QuestionResponseDTO response = questionService.createGeneralQuestion(
            token,
            userId,
            file,
            questionCreateRequestDTO
            );

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @PutMapping("/customers/{userId}/{inquiryId}/{questionId}")
    @Operation(summary = "문의별 질문 수정", description = "문의별 질문을 수정한다.")
    public ResponseEntity<JsonResult> updateQuestion(
        @RequestHeader("Authorization") String token,
        @PathVariable Long userId,
        @PathVariable Long inquiryId,
        @PathVariable Long questionId,
        @RequestPart(value = "files", required = false) MultipartFile file,
        @RequestPart("question") QuestionUpdateRequestDTO questionUpdateRequestDTO
    ) {
        QuestionResponseDTO response = questionService.updateInquiryQuestionById(
            token,
            userId,
            inquiryId,
            questionId,
            file,
            questionUpdateRequestDTO
        );

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @PutMapping("/customers/{userId}/{questionId}")
    @Operation(summary = "기타 질문 수정", description = "기타 질문을 수정한다.")
    public ResponseEntity<JsonResult> updateQuestion(
        @RequestHeader("Authorization") String token,
        @PathVariable Long userId,
        @PathVariable Long questionId,
        @RequestPart(value = "files", required = false) MultipartFile file,
        @RequestPart("question") QuestionUpdateRequestDTO questionUpdateRequestDTO
    ) {
        QuestionResponseDTO response = questionService.updateGeneralQuestion(
            token,
            userId,
            questionId,
            file,
            questionUpdateRequestDTO
        );

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @DeleteMapping("/customers/{userId}/{questionId}")
    @Operation(summary = "질문 삭제(고객사용)", description = "고객사가 작성한 질문을 삭제한다.")
    public ResponseEntity<CommonResult> deleteQuestionById(
        @RequestHeader("Authorization") String token,
        @PathVariable Long userId,
        @PathVariable Long questionId
    ) {
        questionService.deleteQuestionById(token, userId, questionId);

        return ResponseEntity.ok(ResponseFactory.getSuccessResult());
    }

    @DeleteMapping("/managers/{questionId}")
    @Operation(summary = "질문 삭제(담당자용)", description = "담당자가 고객사의 질문을 삭제한다.")
    public ResponseEntity<CommonResult> deleteQuestionById(
        @RequestHeader("Authorization") String token,
        @PathVariable Long questionId
    ) {
        questionService.deleteQuestionById(token, questionId);

        return ResponseEntity.ok(ResponseFactory.getSuccessResult());
    }
}
