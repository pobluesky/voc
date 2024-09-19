package com.pobluesky.voc.answer.controller;

import com.pobluesky.voc.answer.dto.request.AnswerCreateRequestDTO;
import com.pobluesky.voc.answer.dto.request.AnswerUpdateRequestDTO;
import com.pobluesky.voc.answer.dto.response.AnswerResponseDTO;
import com.pobluesky.voc.answer.service.AnswerService;
import com.pobluesky.voc.global.util.ResponseFactory;
import com.pobluesky.voc.global.util.model.CommonResult;
import com.pobluesky.voc.global.util.model.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/answers")
public class AnswerController {

    private final AnswerService answerService;

    @GetMapping("/managers")
    @Operation(summary = "답변 전체 조회(담당자)", description = "등록된 모든 답변을 조회한다. 답변 대기 및 답변 완료 현황을 알 수 있다.")
    public ResponseEntity<JsonResult> getAnswersForManager(
        @RequestHeader("Authorization") String token
    ) {
        List<AnswerResponseDTO> response = answerService.getAnswers(token);

        return ResponseEntity.status((HttpStatus.OK))
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @GetMapping("/managers/{questionId}")
    @Operation(summary = "질문별 답변 상세 조회(담당자)", description = "질문에 대한 상세 답변을 질문 번호로 조회한다.")
    public ResponseEntity<JsonResult> getAnswerByQuestionIdForManager(
        @RequestHeader("Authorization") String token,
        @PathVariable Long questionId
    ) {
        AnswerResponseDTO response = answerService.getAnswerByQuestionIdForManager(
            token,
            questionId
        );

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @GetMapping("/customers/{userId}")
    @Operation(
        summary = "답변 전체 조회(고객사)",
        description = "특정 고객사의 모든 질문에 대한 모든 답변을 조회한다. 답변 대기 및 답변 완료 현황을 알 수 있다."
    )
    public ResponseEntity<JsonResult> getAnswerByUserId(
        @RequestHeader("Authorization") String token,
        @PathVariable Long userId
    ) {
        List<AnswerResponseDTO> response = answerService.getAnswerByUserId(token, userId);

        return ResponseEntity.status((HttpStatus.OK))
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @GetMapping("/customers/{userId}/{questionId}")
    @Operation(summary = "질문별 답변 상세 조회(고객사)", description = "질문에 대한 상세 답변을 질문 번호로 조회한다.")
    public ResponseEntity<JsonResult> getAnswerByQuestionId(
        @RequestHeader("Authorization") String token,
        @PathVariable Long userId,
        @PathVariable Long questionId
    ) {
        AnswerResponseDTO response = answerService.getAnswerByQuestionId(
            token,
            userId,
            questionId
        );

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @PostMapping("/managers/{questionId}")
    @Operation(summary = "질문별 답변 작성(담당자)", description = "질문 번호로 질문을 검색하고 답변을 작성한다.")
    public ResponseEntity<JsonResult> createAnswer(
        @RequestHeader("Authorization") String token,
        @PathVariable Long questionId,
        @RequestPart(value = "files", required = false) MultipartFile file,
        @RequestPart("answer") AnswerCreateRequestDTO answerCreateRequestDTO) {
        AnswerResponseDTO response = answerService.createAnswer(
            token,
            questionId,
            file,
            answerCreateRequestDTO
        );

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @PutMapping("/managers/{questionId}")
    @Operation(summary = "질문별 답변 수정(담당자)", description = "질문 번호로 질문을 검색하고 답변을 수정한다.")
    public ResponseEntity<JsonResult> updateAnswer(
        @RequestHeader("Authorization") String token,
        @PathVariable Long questionId,
        @RequestPart(value = "files", required = false) MultipartFile file,
        @RequestPart("answer") AnswerUpdateRequestDTO answerUpdateRequestDTO
    ) {
        AnswerResponseDTO response = answerService.updateAnswerById(
            token,
            questionId,
            file,
            answerUpdateRequestDTO
        );

        return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseFactory.getSuccessJsonResult(response));
    }

    @DeleteMapping("/managers/{questionId}")
    @Operation(summary = "답변 삭제(담당자)", description = "답변을 삭제한다.")
    public ResponseEntity<CommonResult> deleteAnswerByID(
        @RequestHeader("Authorization") String token,
        @PathVariable Long questionId
    ) {
        answerService.deleteAnswerById(token, questionId);

        return ResponseEntity.ok(ResponseFactory.getSuccessResult());
    }
}
