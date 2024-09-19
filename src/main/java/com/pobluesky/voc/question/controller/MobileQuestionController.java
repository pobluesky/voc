package com.pobluesky.voc.question.controller;

import com.pobluesky.voc.question.dto.response.MobileQuestionSummaryResponseDTO;
import com.pobluesky.voc.question.service.QuestionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/mobile/api/questions")
public class MobileQuestionController {

    private final QuestionService questionService;

    @GetMapping
    public List<MobileQuestionSummaryResponseDTO> getAllQuestions() {

        return questionService.getAllQuestions();
    }

    @GetMapping("/{questionId}")
    public MobileQuestionSummaryResponseDTO getQuestionById(@PathVariable Long questionId) {

        return questionService.getQuestionById(questionId);
    }
}
