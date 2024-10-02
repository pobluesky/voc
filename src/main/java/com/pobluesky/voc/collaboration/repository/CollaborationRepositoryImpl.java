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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class CollaborationRepositoryImpl implements CollaborationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final UserClient userClient;

    @Override
    public Page<CollaborationSummaryResponseDTO> findAllCollaborationsRequest(
        Pageable pageable,
        Long colId,
        ColStatus colStatus,
        String colReqManager,  // 필터링할 manager 이름
        Long colReqId,
        String colResManager,
        Long colResId,
        LocalDate startDate,
        LocalDate endDate,
        String sortBy) {

        // 1. 기본적으로 Collaboration 정보를 조회 (colReqManager 이름 필터링 없이)
        List<Collaboration> collaborations = queryFactory
            .selectFrom(collaboration)
            .where(
                colIdEq(colId),        // colId 조건 추가
                colStatusEq(colStatus),
                colReqIdEq(colReqId),
                colResIdEq(colResId),
                createdDateBetween(startDate, endDate)
            )
            .orderBy(getOrderSpecifier(sortBy))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        // 2. 조회된 Collaboration 목록의 Manager 정보를 가져오기 위한 userId 목록 생성
        List<CollaborationSummaryResponseDTO> content = collaborations.stream()
            .map(c -> {
                // Feign 클라이언트를 통해 colRequestId로 Manager 정보 가져오기
                Manager colReqManagerInfo = null;
                Manager colResManagerInfo = null;
                try {
                    if (c.getColRequestId() != null) {
                        colReqManagerInfo = userClient.getManagerByIdWithoutToken(c.getColRequestId()).getData();
                    }
                    if (c.getColResponseId() != null) {
                        colResManagerInfo = userClient.getManagerByIdWithoutToken(c.getColResponseId()).getData();
                    }
                } catch (Exception e) {
                    // 예외 처리 (예: 로그 기록)
                    colReqManagerInfo = null;
                    colResManagerInfo = null;
                }

                // DTO로 변환
                return CollaborationSummaryResponseDTO.builder()
                    .colId(c.getColId())
                    .questionId(c.getQuestion() != null ? c.getQuestion().getQuestionId() : null)
                    .colReqManager(colReqManagerInfo != null ? colReqManagerInfo.getName() : null)
                    .colId(colReqManagerInfo != null ? colReqManagerInfo.getUserId() : null)
                    .colResManager(colResManagerInfo != null ? colResManagerInfo.getName() : null)
                    .colId(colResManagerInfo != null ? colResManagerInfo.getUserId() : null)
                    .colStatus(c.getColStatus())
                    .colContents(c.getColContents())
                    .createdDate(c.getCreatedDate())
                    .build();
            })
            // 3. 메모리 내에서 colReqManager 이름으로 필터링
            .filter(dto -> {
                if (StringUtils.hasText(colReqManager)) {
                    return dto.colReqManager() != null && dto.colReqManager().equalsIgnoreCase(colReqManager);
                }
                if (StringUtils.hasText(colResManager)) {
                    return dto.colResManager() != null && dto.colResManager().equalsIgnoreCase(colResManager);
                }
                return true; // colReqManager가 없으면 필터링하지 않음
            })
            .collect(Collectors.toList());

        // 4. 카운트 쿼리 (전체 Collaboration 수를 기준으로 설정)
        long total = content.size();

        // 5. 페이징된 CollaborationSummaryResponseDTO 리스트 반환
        return new PageImpl<>(content, pageable, total);
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

    private BooleanExpression colIdEq(Long colId) {
        return colId != null ? collaboration.colId.eq(colId) : null;
    }

    private BooleanExpression colStatusEq(ColStatus colStatus) {
        return colStatus != null ? collaboration.colStatus.eq(colStatus) : null;
    }

//    private BooleanExpression colReqManagerEq(String colReqManager) {
//        return StringUtils.hasText(colReqManager) ? manager.name.eq(colReqManager) : null;
//    }

    private BooleanExpression colReqIdEq(Long colReqId) {
        return colReqId != null ? collaboration.colRequestId.eq(colReqId) : null;
    }

    private BooleanExpression colResIdEq(Long colResId) {
        return colResId != null ? collaboration.colResponseId.eq(colResId) : null;
    }

    private BooleanExpression createdDateBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }

        DateTemplate<LocalDate> dateTemplate = Expressions.dateTemplate(
            LocalDate.class,
            "DATE({0})",
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
