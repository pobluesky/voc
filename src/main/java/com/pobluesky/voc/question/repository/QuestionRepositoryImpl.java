package com.pobluesky.voc.question.repository;


import static com.pobluesky.voc.answer.entity.QAnswer.answer;
import static com.pobluesky.voc.question.entity.QQuestion.question;

import com.pobluesky.voc.feign.Customer;
import com.pobluesky.voc.feign.Manager;
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
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;


@RequiredArgsConstructor
public class QuestionRepositoryImpl implements QuestionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final UserClient userClient;

    @Override
    public Page<QuestionSummaryResponseDTO> findQuestionsByManager(
        Pageable pageable,
        QuestionStatus status,
        QuestionType type,
        String title,
        Long questionId,
        String customerName,
        Boolean isActivated,
        LocalDate startDate,
        LocalDate endDate,
        Long managerId,
        String sortBy
    ) {
        // 1. Question 목록 조회
        List<Question> questions = queryFactory
            .selectFrom(question)
            .leftJoin(question.answer, answer)
            .where(
                statusEq(status),
                typeEq(type),
                titleContains(title),
                questionIdEq(questionId),
                createdDateBetween(startDate, endDate),
                isActivatedEq(isActivated),
                managerIdEq(managerId),
                createdDateBetween(startDate, endDate)
            )
            .orderBy(getOrderSpecifier(sortBy))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        // 2. 조회된 Question 목록을 처리하며 필요한 Customer, Manager 정보를 Feign 클라이언트를 통해 가져옴
        List<QuestionSummaryResponseDTO> content = questions.stream()
            .map(q -> {
                // Feign 클라이언트를 통해 Customer 정보 가져오기
                Customer customer = null;
                if (q.getUserId() != null) {
                    try {
                        customer = userClient.getCustomerByIdWithoutToken(q.getUserId()).getData();
                    } catch (Exception e) {
                        // 예외 처리 로직 (예: 로그 남기기)
                    }
                }

                // Feign 클라이언트를 통해 Manager 정보 가져오기
                Manager manager = null;
                if (q.getAnswer() != null && q.getAnswer().getManagerId() != null) {
                    try {
                        manager = userClient.getManagerByIdWithoutToken(q.getAnswer().getManagerId()).getData();
                    } catch (Exception e) {
                        // 예외 처리 로직 (예: 로그 남기기)
                    }
                }

                // DTO로 변환
                return QuestionSummaryResponseDTO.builder()
                    .questionId(q.getQuestionId())
                    .title(q.getTitle())
                    .status(q.getStatus())
                    .type(q.getType())
                    .contents(q.getContents())
                    .customerName(customer != null ? customer.getCustomerName() : null)
                    .questionCreatedAt(q.getCreatedDate())
                    .answerCreatedAt(q.getAnswer() != null ? q.getAnswer().getCreatedDate() : null)
                    .managerId(manager != null ? manager.getUserId() : null)
                    .isActivated(q.getIsActivated())
                    .build();
            })
            // 3. customerName 필터링 추가
            .filter(dto -> {
                // customerName이 입력된 경우 필터링
                if (StringUtils.hasText(customerName)) {
                    return dto.customerName() != null && dto.customerName().contains(customerName);
                }
                return true; // customerName이 없는 경우 모든 데이터 유지
            })
            .collect(Collectors.toList());

        // 4. customerName 필터링 후 데이터의 크기를 기준으로 페이지 계산
        int totalFiltered = content.size();

        // 5. 페이징된 QuestionSummaryResponseDTO 리스트 반환
        return new PageImpl<>(content, pageable, totalFiltered);
    }

    @Override
    public Page<QuestionSummaryResponseDTO> findQuestionsByCustomer(
        Pageable pageable,
        Long userId,
        QuestionStatus status,
        QuestionType type,
        String title,
        Long questionId,
        LocalDate startDate,
        LocalDate endDate,
        String sortBy
    ) {
        // 1. Question 목록 조회
        List<Question> questions = queryFactory
            .selectFrom(question)
            .leftJoin(question.answer, answer)
            .where(
                question.userId.eq(userId), // userId 조건 추가
                statusEq(status),
                typeEq(type),
                titleContains(title),
                questionIdEq(questionId),
                isActivatedEq(true),
                createdDateBetween(startDate, endDate)
            )
            .orderBy(getOrderSpecifier(sortBy))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        // 2. 조회된 Question 목록을 처리하며 필요한 Customer 정보를 Feign 클라이언트를 통해 가져옴
        List<QuestionSummaryResponseDTO> content = questions.stream()
            .map(q -> {
                // Feign 클라이언트를 통해 Customer 정보 가져오기
                Customer customer = null;
                if (q.getUserId() != null) {
                    try {
                        customer = userClient.getCustomerByIdWithoutToken(q.getUserId()).getData();
                    } catch (Exception e) {
                        // 예외 처리 로직 (예: 로그 남기기)
                    }
                }

                // Feign 클라이언트를 통해 Manager 정보 가져오기
                Manager manager = null;
                if (q.getAnswer() != null && q.getAnswer().getManagerId() != null) {
                    try {
                        manager = userClient.getManagerByIdWithoutToken(q.getAnswer().getManagerId()).getData();
                    } catch (Exception e) {
                        // 예외 처리 로직 (예: 로그 남기기)
                    }
                }

                // DTO로 변환
                return QuestionSummaryResponseDTO.builder()
                    .questionId(q.getQuestionId())
                    .title(q.getTitle())
                    .status(q.getStatus())
                    .type(q.getType())
                    .contents(q.getContents())
                    .customerName(customer != null ? customer.getCustomerName() : null)
                    .questionCreatedAt(q.getCreatedDate())
                    .answerCreatedAt(q.getAnswer() != null ? q.getAnswer().getCreatedDate() : null)
                    .managerId(manager != null ? manager.getUserId() : null)
                    .isActivated(q.getIsActivated())
                    .build();
            })
            .collect(Collectors.toList());

        // 3. 페이징된 QuestionSummaryResponseDTO 리스트 반환
        return new PageImpl<>(content, pageable, content.size());
    }

    @Override
    public List<QuestionSummaryResponseDTO> findQuestionsBySearch(
        String sortBy,
        QuestionStatus status,
        QuestionType type,
        String title,
        String customerName,
        LocalDate startDate,
        LocalDate endDate
    ) {
        // 1. 필요한 필드만 가져오기 (customerId와 managerId만 가져옴)
        List<Question> questions = queryFactory
            .selectFrom(question)
            .leftJoin(question.answer, answer)
            .where(
                statusEq(status),
                typeEq(type),
                titleContains(title),
                createdDateBetween(startDate, endDate)
            )
            .orderBy(getOrderSpecifier(sortBy))
            .fetch();

        // 2. FeignClient를 통해 Customer 및 Manager 정보 가져오기
        List<QuestionSummaryResponseDTO> content = questions.stream()
            .map(q -> {
                // FeignClient를 통해 Customer 정보 가져오기
                Customer customer = null;
                if (q.getUserId() != null) {
                    try {
                        customer = userClient.getCustomerByIdWithoutToken(q.getUserId()).getData();
                    } catch (Exception e) {
                        // 예외 처리 로직 (예: 로그 남기기)
                    }
                }

                // FeignClient를 통해 Manager 정보 가져오기
                Manager manager = null;
                if (q.getAnswer() != null && q.getAnswer().getManagerId() != null) {
                    try {
                        manager = userClient.getManagerByIdWithoutToken(q.getAnswer().getManagerId()).getData();
                    } catch (Exception e) {
                        // 예외 처리 로직 (예: 로그 남기기)
                    }
                }

                // DTO로 변환하여 리스트에 추가
                return QuestionSummaryResponseDTO.builder()
                    .questionId(q.getQuestionId())
                    .title(q.getTitle())
                    .status(q.getStatus())
                    .type(q.getType())
                    .contents(q.getContents())
                    .customerName(customer != null ? customer.getCustomerName() : null)
                    .questionCreatedAt(q.getCreatedDate())
                    .answerCreatedAt(q.getAnswer() != null ? q.getAnswer().getCreatedDate() : null)
                    .managerId(manager != null ? manager.getUserId() : null)
                    .isActivated(q.getIsActivated())
                    .build();
            })
            // 3. customerName 필터링
            .filter(dto -> {
                if (StringUtils.hasText(customerName)) {
                    return dto.customerName() != null && dto.customerName().contains(customerName);
                }
                return true; // 필터링 조건이 없으면 모든 항목 유지
            })
            .collect(Collectors.toList());

        return content;
    }

    private JPAQuery<Question> getCountQueryForManager(
        QuestionStatus status,
        QuestionType type,
        String title,
        Long questionId,
        Boolean isActivated,
        Long managerId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        // customerNameContains 제거, customerName 필터링 제외
        return queryFactory
            .selectFrom(question)
            .where(
                statusEq(status),
                typeEq(type),
                titleContains(title),
                questionIdEq(questionId),
                isActivatedEq(isActivated),
                managerIdEq(managerId),
                createdDateBetween(startDate, endDate)
            );
    }

    private JPAQuery<Question> getCountQueryForCustomer(
        Long userId,
        QuestionStatus status,
        QuestionType type,
        String title,
        Long questionId,
        Long managerId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return queryFactory
            .selectFrom(question)
            .where(
                question.userId.eq(userId),
                statusEq(status),
                typeEq(type),
                titleContains(title),
                questionIdEq(questionId),
                isActivatedEq(true),
                managerIdEq(managerId),
                createdDateBetween(startDate, endDate)
            );
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

    private BooleanExpression isActivatedEq(Boolean isActivated) {
        return isActivated != null ? question.isActivated.eq(isActivated) : null;
    }

    private BooleanExpression managerIdEq(Long managerId) {
        return managerId != null ? answer.managerId.eq(managerId) : null;
    }

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
