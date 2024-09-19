package com.pobluesky.voc.question.dto.response;


import com.pobluesky.voc.feign.Customer;
import com.pobluesky.voc.feign.UserClient;
import com.pobluesky.voc.question.entity.Question;
import lombok.Builder;

@Builder
public record MobileQuestionSummaryResponseDTO (

        Long questionId,

        String customer,

        String title,

        String status,

        String type,

        String contents
) {
    public static MobileQuestionSummaryResponseDTO from(Question question, UserClient userClient) {
        Customer customer = userClient.getCustomerByIdWithoutToken(question.getUserId()).getData();

        return MobileQuestionSummaryResponseDTO.builder()
                .questionId(question.getQuestionId())
                .customer(customer.getName())
                .title(question.getTitle())
                .status(question.getStatus().getStatus())
                .type(question.getType().getType())
                .contents(question.getContents())
                .build();
    }
}
