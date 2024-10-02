package com.pobluesky.voc.question.dto.response;


import com.pobluesky.voc.feign.Customer;
import com.pobluesky.voc.feign.Inquiry;
import com.pobluesky.voc.feign.InquiryClient;
import com.pobluesky.voc.feign.UserClient;
import com.pobluesky.voc.global.util.model.JsonResult;
import com.pobluesky.voc.question.entity.Question;
import lombok.Builder;

import java.util.Optional;

@Builder
public record MobileQuestionSummaryResponseDTO (

        Optional<Long> inquiryId,

        Long questionId,

        String customer,

        String title,

        String status,

        String type,

        String contents
) {
    public static MobileQuestionSummaryResponseDTO from(Question question, UserClient userClient, InquiryClient inquiryClient) {
        Customer customer = userClient.getCustomerByIdWithoutToken(question.getUserId()).getData();
        JsonResult<Inquiry> inquiryResult = inquiryClient.getInquiryByIdWithoutToken(question.getInquiryId());
        Inquiry inquiry = inquiryResult != null ? inquiryResult.getData() : null;

        return MobileQuestionSummaryResponseDTO.builder()
                .inquiryId(Optional.ofNullable(inquiry)
                    .map(Inquiry::getInquiryId))
                .questionId(question.getQuestionId())
                .customer(customer.getName())
                .title(question.getTitle())
                .status(question.getStatus().getStatus())
                .type(question.getType().getType())
                .contents(question.getContents())
                .build();
    }

    public static MobileQuestionSummaryResponseDTO toMobileResponseDTO(QuestionSummaryResponseDTO questionSummary) {

        return MobileQuestionSummaryResponseDTO.builder()
                .inquiryId(Optional.ofNullable(questionSummary.managerId()))
                .questionId(questionSummary.questionId())
                .customer(questionSummary.customerName())
                .title(questionSummary.title())
                .status(questionSummary.status().getStatus())
                .type(questionSummary.type().getType())
                .contents(questionSummary.contents())
                .build();
    }
}
