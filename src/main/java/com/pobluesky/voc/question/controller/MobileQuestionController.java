package com.pobluesky.voc.question.controller;

import com.pobluesky.voc.question.dto.response.MobileQuestionSummaryResponseDTO;
import com.pobluesky.voc.question.entity.QuestionStatus;
import com.pobluesky.voc.question.entity.QuestionType;
import com.pobluesky.voc.question.service.QuestionService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/search")
    public List<MobileQuestionSummaryResponseDTO> getQuestionBySearch(
            @RequestParam(defaultValue = "LATEST") String sortBy,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        QuestionStatus statusEnum =  (status != null) ? QuestionStatus.fromString(status) : null;
        QuestionType questionTypeEnum = (type != null) ? QuestionType.fromString(type) : null;

        return questionService.getQuestionsBySearch(
                sortBy,
                statusEnum,
                questionTypeEnum,
                title,
                customerName,
                startDate,
                endDate
        );
    }
}
