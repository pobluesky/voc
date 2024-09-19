package com.pobluesky.voc.question.repository;


import static com.pobluesky.voc.answer.entity.QAnswer.answer;
import static com.pobluesky.voc.question.entity.QQuestion.question;

import com.pobluesky.voc.feign.Customer;
import com.pobluesky.voc.feign.UserClient;
import com.pobluesky.voc.global.error.CommonException;
import com.pobluesky.voc.global.error.ErrorCode;
import com.pobluesky.voc.question.dto.response.QuestionSummaryResponseDTO;
import com.pobluesky.voc.question.entity.Question;
import com.pobluesky.voc.question.entity.QuestionStatus;
import com.pobluesky.voc.question.entity.QuestionType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

import org.springframework.util.StringUtils;


@RequiredArgsConstructor
public class QuestionRepositoryImpl implements QuestionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final UserClient userClient;

    @Override
    public List<QuestionSummaryResponseDTO> findAllQuestionsByCustomerWithoutPaging(
        Long userId,
        QuestionStatus status,
        QuestionType type,
        String title,
        Long questionId,
        LocalDate startDate,
        LocalDate endDate,
        String sortBy
    ) {
        List<Question> questions = queryFactory
            .selectFrom(question)
            .leftJoin(question.answer, answer)
            .where(
                question.userId.eq(userId),
                statusEq(status),
                typeEq(type),
                titleContains(title),
                questionIdEq(questionId),
                createdDateBetween(startDate, endDate)
            )
            .orderBy(getOrderSpecifier(sortBy))
            .fetch();

        // 2. 조회된 Question 목록을 처리하며 Customer 정보를 Feign 클라이언트를 통해 가져옴
        return questions.stream()
            .map(q -> {
                // Feign을 사용해 Customer 정보를 가져옴
                Customer customer = userClient.getCustomerByIdWithoutToken(q.getUserId()).getData();

                // DTO로 변환
                return QuestionSummaryResponseDTO.builder()
                    .questionId(q.getQuestionId())
                    .title(q.getTitle())
                    .status(q.getStatus())
                    .type(q.getType())
                    .contents(q.getContents())
                    .customerName(customer != null ? customer.getCustomerName() : null)  // Feign을 통해 조회된 고객 정보
                    .questionCreatedAt(q.getCreatedDate())
                    .answerCreatedAt(q.getAnswer() != null ? q.getAnswer().getCreatedDate() : null)
                    .isActivated(q.getIsActivated())
                    .build();
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<QuestionSummaryResponseDTO> findAllQuestionsByManagerWithoutPaging(
        QuestionStatus status,
        QuestionType type,
        String title,
        Long questionId,
        String customerName,
        LocalDate startDate,
        LocalDate endDate,
        String sortBy
    ) {
        List<Question> questions = queryFactory
            .selectFrom(question)
            .leftJoin(question.answer, answer)
            .where(
                statusEq(status),
                typeEq(type),
                titleContains(title),
                questionIdEq(questionId),
                createdDateBetween(startDate, endDate)
            )
            .orderBy(getOrderSpecifier(sortBy))
            .fetch();
        // 2. 조회된 Question 목록을 처리하며 Customer 정보를 Feign 클라이언트를 통해 가져옴
        return questions.stream()
            .map(q -> {
                // Feign을 사용해 Customer 정보를 가져옴
                Customer customer = userClient.getCustomerByIdWithoutToken(q.getUserId()).getData();

                // DTO로 변환
                return QuestionSummaryResponseDTO.builder()
                    .questionId(q.getQuestionId())
                    .title(q.getTitle())
                    .status(q.getStatus())
                    .type(q.getType())
                    .contents(q.getContents())
                    .customerName(customer != null ? customer.getCustomerName() : null)  // Feign을 통해 조회된 고객 정보
                    .questionCreatedAt(q.getCreatedDate())
                    .answerCreatedAt(q.getAnswer() != null ? q.getAnswer().getCreatedDate() : null)
                    .isActivated(q.getIsActivated())
                    .build();
            })
            // 3. customerName이 존재하면 메모리에서 필터링
            .filter(dto -> {
                if (StringUtils.hasText(customerName)) {
                    // dto.customerName()이 null이 아닐 때만 contains() 호출
                    return dto.customerName() != null && dto.customerName().contains(customerName);
                }
                return true;  // customerName이 없는 경우 필터링하지 않음
            })
            .collect(Collectors.toList());
    }

    private OrderSpecifier<?>[] getOrderSpecifier(String sortBy) {
        switch (sortBy) {
            case "LATEST":
                return new OrderSpecifier[]{
                    question.createdDate.desc().nullsLast(),
                    question.questionId.desc()
                };
            case "OLDEST":
                return new OrderSpecifier[]{
                    question.createdDate.asc().nullsFirst(),
                    question.questionId.asc()
                };
            case "TYPE":
                return new OrderSpecifier[]{
                    question.type.asc(),
                    question.createdDate.desc()
                };
            default:
                throw new CommonException(ErrorCode.INVALID_ORDER_CONDITION);
        }
    }

    private BooleanExpression statusEq(QuestionStatus status) {
        return status != null ? question.status.eq(status) : null;
    }

    private BooleanExpression typeEq(QuestionType type) {
        return type != null ? question.type.eq(type) : null;
    }

    private BooleanExpression titleContains(String title) {
        return StringUtils.hasText(title) ? question.title.contains(title) : null;
    }

    private BooleanExpression questionIdEq(Long questionId) {
        return questionId != null ? question.questionId.eq(questionId) : null;
    }

//    private BooleanExpression customerNameContains(String customerName) {
//        return StringUtils.hasText(customerName) ? customer.customerName.contains(customerName) : null;
//    }

    private BooleanExpression createdDateBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }

        DateTemplate<LocalDate> dateTemplate = Expressions.dateTemplate(
            LocalDate.class,
            "CAST({0} AS DATE)",
            question.createdDate
        );

        if (startDate != null && endDate != null) {
            return dateTemplate.between(startDate, endDate);
        } else if (startDate != null) {
            return dateTemplate.goe(startDate);
        } else {
            return dateTemplate.loe(endDate);
        }
    }
}
