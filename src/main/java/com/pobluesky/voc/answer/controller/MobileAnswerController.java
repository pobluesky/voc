package com.pobluesky.voc.answer.controller;

import com.pobluesky.voc.answer.dto.response.MobileAnswerSummaryResponseDTO;
import com.pobluesky.voc.answer.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mobile/api/answers")
public class MobileAnswerController {

    private final AnswerService answerService;

    @GetMapping("/{questionId}")
    public MobileAnswerSummaryResponseDTO getAnswerByQuestionId(@PathVariable Long questionId) {
        return answerService.getAnswerByQuestionId(questionId);
    }
}
