package com.pobluesky.voc.collaboration.service;

import com.pobluesky.voc.collaboration.dto.request.CollaborationCreateRequestDTO;
import com.pobluesky.voc.collaboration.dto.request.CollaborationModifyRequestDTO;
import com.pobluesky.voc.collaboration.dto.request.CollaborationUpdateRequestDTO;
import com.pobluesky.voc.collaboration.dto.response.CollaborationDetailResponseDTO;
import com.pobluesky.voc.collaboration.dto.response.CollaborationResponseDTO;
import com.pobluesky.voc.collaboration.dto.response.CollaborationSummaryResponseDTO;
import com.pobluesky.voc.collaboration.entity.ColStatus;
import com.pobluesky.voc.collaboration.entity.Collaboration;
import com.pobluesky.voc.collaboration.repository.CollaborationRepository;
import com.pobluesky.voc.feign.FileClient;
import com.pobluesky.voc.feign.Manager;
import com.pobluesky.voc.feign.UserClient;
import com.pobluesky.voc.feign.FileInfo;
import com.pobluesky.voc.global.error.CommonException;
import com.pobluesky.voc.global.error.ErrorCode;
import com.pobluesky.voc.global.security.UserRole;
import com.pobluesky.voc.question.entity.Question;
import com.pobluesky.voc.question.entity.QuestionStatus;
import com.pobluesky.voc.question.repository.QuestionRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pobluesky.voc.global.redisson.DistributedLock;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CollaborationService {

    private final CollaborationRepository collaborationRepository;

    private final QuestionRepository questionRepository;

    private final UserClient userClient;

    private final FileClient fileClient;

    // 협업 조회 without paging
    @Transactional(readOnly = true)
    public Page<CollaborationSummaryResponseDTO> getAllCollaborations(
        String token,
        int page,
        int size,
        String sortBy,
        Long colId,
        ColStatus colStatus,
        String colReqManager,
        Long colReqId,
        String colResManager,
        Long colResId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        Long userId = userClient.parseToken(token);

        Manager manager = userClient.getManagerByIdWithoutToken(userId).getData();
        if(manager == null) {
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        if(manager.getRole() == UserRole.CUSTOMER)
            throw new CommonException(ErrorCode.UNAUTHORIZED_USER_MANAGER);

        Pageable pageable = PageRequest.of(page, size);

        return collaborationRepository.findAllCollaborationsRequest(
            pageable,
            colId,
            colStatus,
            colReqManager,
            colReqId,
            colResManager,
            colResId,
            startDate,
            endDate,
            sortBy
        );
    }

    @Transactional(readOnly = true)
    public CollaborationDetailResponseDTO getCollaborationById(
        String token,
        Long questionId,
        Long collaborationId
    ) {
        Long userId = userClient.parseToken(token);

        Manager manager = userClient.getManagerByIdWithoutToken(userId).getData();
        if(manager == null) {
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND));

        Collaboration collaboration = collaborationRepository.findByIdAndQuestion(
                collaborationId,
                question
            ).orElseThrow(() -> new CommonException(ErrorCode.COLLABORATION_NOT_FOUND));

        return CollaborationDetailResponseDTO.from(collaboration,userClient);
    }

    @Transactional
    @DistributedLock(key = "#lockName")
    public CollaborationResponseDTO createCollaboration(
        String token,
        String lockName,
        Long questionId,
        MultipartFile file,
        CollaborationCreateRequestDTO requestDTO
        ) {
        Long userId = userClient.parseToken(token);

        if(!userClient.managerExists(userId)){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND));

        Manager reqManager = userClient.getManagerByIdWithoutToken(requestDTO.colReqId()).getData();
        if(reqManager == null) {
            throw new CommonException(ErrorCode.REQ_MANAGER_NOT_FOUND);
        }

        Manager resManager = userClient.getManagerByIdWithoutToken(requestDTO.colResId()).getData();
        if(reqManager == null) {
            throw new CommonException(ErrorCode.RES_MANAGER_NOT_FOUND);
        }

        String fileName = null;
        String filePath = null;

        if (file != null) {
            FileInfo fileInfo = fileClient.uploadFile(file);
            fileName = fileInfo.getOriginName();
            filePath = fileInfo.getStoredFilePath();
        }

        Collaboration collaborationEntity = requestDTO.toCollaborationEntity(
            reqManager.getUserId(),
            resManager.getUserId(),
            question,
            fileName,
            filePath
        );

        Collaboration savedCollaboration = collaborationRepository.save(collaborationEntity);

        return CollaborationResponseDTO.from(savedCollaboration,userClient);
    }

    @Transactional
    public CollaborationDetailResponseDTO updateCollaborationStatus(
        String token,
        Long collaborationId,
        MultipartFile file,
        CollaborationUpdateRequestDTO requestDTO
    ) {
        Long userId = userClient.parseToken(token);

        Collaboration collaboration = validateCollaboration(collaborationId);

        if (collaboration.getColStatus() == ColStatus.INPROGRESS) {
            throw new CommonException(ErrorCode.COLLABORATION_STATUS_INPROGRESS);
        }

        if(!userClient.managerExists(userId)){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        Manager reqManager = userClient.getManagerByIdWithoutToken(requestDTO.colReqId()).getData();
        if(reqManager == null) {
            throw new CommonException(ErrorCode.REQ_MANAGER_NOT_FOUND);
        }

        Manager resManager = userClient.getManagerByIdWithoutToken(requestDTO.colResId()).getData();
        if(reqManager == null) {
            throw new CommonException(ErrorCode.RES_MANAGER_NOT_FOUND);
        }

        if (!collaboration.getColRequestId().equals(reqManager.getUserId()) ||
            !collaboration.getColResponseId().equals(resManager.getUserId())) {
            throw new CommonException(ErrorCode.COLLABORATION_INFO_MISMATCH);
        }

        if(!userId.equals(collaboration.getColResponseId()))
            throw new CommonException(ErrorCode.RESMANAGER_NOT_MACHED);

        collaboration.writeColReply(requestDTO.colReply());
        collaboration.decideCollaboration(requestDTO.isAccepted());

        String fileName = null;
        String filePath = null;

        if (file != null) {
            FileInfo fileInfo = fileClient.uploadFile(file);
            fileName = fileInfo.getOriginName();
            filePath = fileInfo.getStoredFilePath();
        }

        collaboration.updateFiles(fileName, filePath);

        return CollaborationDetailResponseDTO.from(collaboration,userClient);
    }

    @Transactional
    public CollaborationDetailResponseDTO completeCollaboration(
        String token,
        Long collaborationId
    ) {
        Long userId = userClient.parseToken(token);

        if(!userClient.managerExists(userId)){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        Collaboration collaboration = validateCollaboration(collaborationId);

        if(!userId.equals(collaboration.getColResponseId()))
            throw new CommonException(ErrorCode.RESMANAGER_NOT_MACHED);

        collaboration.completeCollaboration();

        return CollaborationDetailResponseDTO.from(collaboration,userClient);
    }

    @Transactional
    public CollaborationDetailResponseDTO modifyCollaboration(
        String token,
        Long collaborationId,
        MultipartFile file,
        CollaborationModifyRequestDTO requestDTO
    ) {
        Long userId = userClient.parseToken(token);

        Collaboration collaboration = validateCollaboration(collaborationId);

        validateRequestManager(collaboration, requestDTO.colReqId(), userId);
        validateResponseManager(collaboration, requestDTO.colResId(), userId);

        collaboration.modifyCollaborationContents(requestDTO.colContents());

        String fileName = collaboration.getFileName();
        String filePath = collaboration.getFilePath();

        boolean isFileDeleted = requestDTO.isFileDeleted() != null && requestDTO.isFileDeleted();

        if (isFileDeleted) {
            fileName = null;
            filePath = null;
        } else if (file != null) {
            FileInfo fileInfo = fileClient.uploadFile(file);
            fileName = fileInfo.getOriginName();
            filePath = fileInfo.getStoredFilePath();
        }

        collaboration.updateFiles(fileName, filePath);

        if (requestDTO.isAccepted() != null) {
            collaboration.updateCollaborationStatus(requestDTO.isAccepted());
        }
        if (requestDTO.colReply() != null) {
            collaboration.modifyColReply(requestDTO.colReply());
        }

        return CollaborationDetailResponseDTO.from(collaboration, userClient);
    }

    // 월별 담당자별 협업 처리 건수
    @Transactional(readOnly = true)
    public Map<String, List<Object[]>> getAverageCountPerMonth(String token) {
        Long userId = userClient.parseToken(token);

        Manager manager = userClient.getManagerByIdWithoutToken(userId).getData();
        if(manager == null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        Map<String, List<Object[]>> results = new HashMap<>();

        results.put("total", collaborationRepository.findAverageCountPerMonth());
        results.put("manager", collaborationRepository.findAverageCountPerMonthByManager(manager.getUserId()));

        return results;
    }

    private Collaboration validateCollaboration(Long collaborationId) {
        Collaboration collaboration = collaborationRepository
            .findById(collaborationId)
            .orElseThrow(() -> new CommonException(ErrorCode.COLLABORATION_NOT_FOUND));

        if (collaboration.getColStatus() == ColStatus.COMPLETE) {
            throw new CommonException(ErrorCode.COLLABORATION_STATUS_COMPLETED);
        }

        Question question = questionRepository.findById(collaboration.getQuestion().getQuestionId())
            .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND));

        if(question.getStatus() == QuestionStatus.COMPLETED) {
            throw new CommonException(ErrorCode.QUESTION_STATUS_COMPLETED);
        }

        return collaboration;
    }

    private void updateFile(Collaboration collaboration, MultipartFile file) {
        String fileName = collaboration.getFileName();
        String filePath = collaboration.getFilePath();

        if (file != null) {
            FileInfo fileInfo = fileClient.uploadFile(file);
            fileName = fileInfo.getOriginName();
            filePath = fileInfo.getStoredFilePath();
        }

        collaboration.updateFiles(fileName, filePath);
    }

    private void validateRequestManager(Collaboration collaboration, Long reqManagerId, Long userId) {
        if (collaboration.getColStatus() == ColStatus.READY) {
            Manager reqManager = userClient.getManagerByIdWithoutToken(reqManagerId).getData();
            if(reqManager == null) {
                throw new CommonException(ErrorCode.REQ_MANAGER_NOT_FOUND);
            }

            if (!userId.equals(reqManager.getUserId())) {
                throw new CommonException(ErrorCode.REQMANAGER_NOT_MACHED);
            }
        }
    }

    private void validateResponseManager(Collaboration collaboration, Long resManagerId, Long userId) {
        if (collaboration.getColStatus() == ColStatus.INPROGRESS) {
            Manager resManager = userClient.getManagerByIdWithoutToken(resManagerId).getData();
            if(resManager == null) {
                throw new CommonException(ErrorCode.REQ_MANAGER_NOT_FOUND);
            }

            if (!userId.equals(resManager.getUserId())) {
                throw new CommonException(ErrorCode.RESMANAGER_NOT_MACHED);
            }
        }
    }
}
