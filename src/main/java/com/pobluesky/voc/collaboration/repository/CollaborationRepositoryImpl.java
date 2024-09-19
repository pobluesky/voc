package com.pobluesky.voc.collaboration.repository;

import static com.pobluesky.voc.collaboration.entity.QCollaboration.collaboration;

import com.pobluesky.voc.collaboration.dto.response.CollaborationSummaryResponseDTO;
import com.pobluesky.voc.collaboration.entity.ColStatus;
import com.pobluesky.voc.collaboration.entity.Collaboration;
import com.pobluesky.voc.feign.Manager;
import com.pobluesky.voc.feign.UserClient;
import com.pobluesky.voc.global.error.CommonException;
import com.pobluesky.voc.global.error.ErrorCode;
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
public class CollaborationRepositoryImpl implements CollaborationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final UserClient userClient;

    @Override
    public List<CollaborationSummaryResponseDTO> findAllCollaborationsRequestWithoutPaging(
        ColStatus colStatus,
        String colReqManager,
        Long colReqId,
        LocalDate startDate,
        LocalDate endDate,
        String sortBy) {

        // 1. 기본적으로 Collaboration 정보를 조회 (Manager 이름 필터링 없이)
        List<Collaboration> collaborations = queryFactory
            .selectFrom(collaboration)
            .where(
                colStatusEq(colStatus),
                colReqIdEq(colReqId),
                createdDateBetween(startDate, endDate)
            )
            .orderBy(getOrderSpecifier(sortBy))
            .fetch();

        // 2. 조회된 Collaboration 목록을 처리하며 Manager 정보를 Feign 클라이언트를 통해 가져옴
        return collaborations.stream()
            .map(c -> {
                // Feign을 사용해 Manager 정보를 가져옴
                Manager manager = userClient.getManagerByIdWithoutToken(c.getColRequestId()).getData(); // manager 정보를 ID 기반으로 가져옴

                // DTO로 변환
                return CollaborationSummaryResponseDTO.builder()
                    .colId(c.getColId())
                    .questionId(c.getQuestion().getQuestionId())
                    .colReqManager(manager != null ? manager.getName() : null)  // Feign으로 조회된 manager 이름
                    .colStatus(c.getColStatus())
                    .colContents(c.getColContents())
                    .createdDate(c.getCreatedDate())
                    .build();
            })
            // 3. colReqManager(이름)으로 필터링
            .filter(dto -> StringUtils.isEmpty(colReqManager) || dto.colReqManager().equalsIgnoreCase(colReqManager))
            .collect(Collectors.toList());
    }

    private OrderSpecifier<?>[] getOrderSpecifier(String sortBy) {
        switch (sortBy) {
            case "LATEST":
                return new OrderSpecifier[]{
                    collaboration.createdDate.desc().nullsLast(),
                    collaboration.colId.desc()
                };
            case "OLDEST":
                return new OrderSpecifier[]{
                    collaboration.createdDate.asc().nullsFirst(),
                    collaboration.colId.asc()
                };
            default:
                throw new CommonException(ErrorCode.INVALID_ORDER_CONDITION);
        }
    }

    private BooleanExpression colStatusEq(ColStatus colStatus) {
        return colStatus != null ? collaboration.colStatus.eq(colStatus) : null;
    }

//    private BooleanExpression colReqManagerEq(String colReqManager) {
//        return StringUtils.hasText(colReqManager) ? manager.name.eq(colReqManager) : null;
//    }

    private BooleanExpression colReqIdEq(Long colReqId) {
        return colReqId != null ? collaboration.colId.eq(colReqId) : null;
    }

    private BooleanExpression createdDateBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }

        DateTemplate<LocalDate> dateTemplate = Expressions.dateTemplate(
            LocalDate.class,
            "CAST({0} AS DATE)",
            collaboration.createdDate
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
