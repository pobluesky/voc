package com.pobluesky.voc.question.service;

import com.pobluesky.voc.collaboration.entity.Collaboration;
import com.pobluesky.voc.collaboration.repository.CollaborationRepository;
import com.pobluesky.voc.feign.Customer;
import com.pobluesky.voc.feign.FileClient;
import com.pobluesky.voc.feign.FileInfo;
import com.pobluesky.voc.feign.Inquiry;
import com.pobluesky.voc.feign.InquiryClient;
import com.pobluesky.voc.feign.Manager;
import com.pobluesky.voc.feign.UserClient;
import com.pobluesky.voc.global.error.CommonException;
import com.pobluesky.voc.global.error.ErrorCode;
import com.pobluesky.voc.question.dto.request.QuestionCreateRequestDTO;
import com.pobluesky.voc.question.dto.request.QuestionUpdateRequestDTO;
import com.pobluesky.voc.question.dto.response.MobileQuestionSummaryResponseDTO;
import com.pobluesky.voc.question.dto.response.QuestionResponseDTO;
import com.pobluesky.voc.question.dto.response.QuestionSummaryResponseDTO;
import com.pobluesky.voc.question.entity.Question;
import com.pobluesky.voc.question.entity.QuestionStatus;
import com.pobluesky.voc.question.entity.QuestionType;
import com.pobluesky.voc.question.repository.QuestionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    private final CollaborationRepository collaborationRepository;

    private final InquiryClient inquiryClient;

    private final UserClient userClient;

    private final FileClient fileClient;

    // 질문 전체 조회 (담당자) without paging
    @Transactional(readOnly = true)
    public Page<QuestionSummaryResponseDTO> getQuestionsByManager(
        String token,
        int page,
        int size,
        String sortBy,
        QuestionStatus status,
        QuestionType type,
        String title,
        Long questionId,
        String customerName,
        Boolean isActivated,
        Long managerId,
        LocalDate startDate,
        LocalDate endDate) {

        validateManager(token);

        Pageable pageable = PageRequest.of(page, size);

        return questionRepository.findQuestionsByManager(
            pageable,
            status,
            type,
            title,
            questionId,
            customerName,
            isActivated,
            startDate,
            endDate,
            managerId,
            sortBy);
    }

    // 질문 전체 조회 (고객사)
    @Transactional(readOnly = true)
    public Page<QuestionSummaryResponseDTO> getQuestionsByCustomer(
        String token,
        Long customerId,
        int page,
        int size,
        String sortBy,
        QuestionStatus status,
        QuestionType type,
        String title,
        Long questionId,
        LocalDate startDate,
        LocalDate endDate) {

        Customer customer = validateCustomer(token);

        validateUserMatch(customer.getUserId(), customerId);

        Pageable pageable = PageRequest.of(page, size);

        return questionRepository.findQuestionsByCustomer(
            pageable,
            customerId,
            status,
            type,
            title,
            questionId,
            startDate,
            endDate,
            sortBy);
    }

    // 질문 번호별 질문 조회 (담당자)
    @Transactional(readOnly = true)
    public QuestionResponseDTO getQuestionByQuestionIdForManager(String token, Long questionId) {
        validateManager(token);

        Question question = validateQuestion(questionId);

        return QuestionResponseDTO.from(question,userClient,inquiryClient);
    }

    // 질문 번호별 질문 조회 (고객사)
    @Transactional(readOnly = true)
    public QuestionResponseDTO getQuestionByQuestionId(String token, Long customerId, Long questionId) {
        Customer customer = validateCustomer(token);

        Question question = validateQuestion(questionId);

        validateUserMatch(customer.getUserId(), customerId);

        validateUserMatch(question.getUserId(), customerId);

        return QuestionResponseDTO.from(question,userClient,inquiryClient);
    }

    // 문의별 질문 작성 (고객사)
    @Transactional
    public QuestionResponseDTO createInquiryQuestion(
        String token,
        Long customerId,
        Long inquiryId,
        MultipartFile file,
        QuestionCreateRequestDTO dto
    ) {
        Customer customer = validateCustomer(token);

        Inquiry inquiry = validateInquiry(inquiryId);

        validateUserMatch(customer.getUserId(), customerId);

        validateInquiryMatch(inquiry, customerId);

        String fileName = null;
        String filePath = null;

        if (file != null) {
            FileInfo fileInfo = fileClient.uploadFile(file);
            fileName = fileInfo.getOriginName();
            filePath = fileInfo.getStoredFilePath();
        }

        Question question = dto.toQuestionEntity(inquiryId, customerId, fileName, filePath);
        Question savedQuestion = questionRepository.save(question);

        return QuestionResponseDTO.from(savedQuestion,userClient,inquiryClient);
    }

    // 타입별 질문 작성 (고객사)
    @Transactional
    public QuestionResponseDTO createGeneralQuestion(
        String token,
        Long customerId,
        MultipartFile file,
        QuestionCreateRequestDTO dto
        ) {
        Customer customer = validateCustomer(token);

        validateUserMatch(customer.getUserId(), customerId);

        String fileName = null;
        String filePath = null;

        if (file != null) {
            FileInfo fileInfo = fileClient.uploadFile(file);
            fileName = fileInfo.getOriginName();
            filePath = fileInfo.getStoredFilePath();
        } else {
            fileName = null;
            filePath = null;
        }

        Question question = dto.toQuestionEntity(null, customerId, fileName, filePath);
        Question savedQuestion = questionRepository.save(question);

        return QuestionResponseDTO.from(savedQuestion,userClient,inquiryClient);
    }

    // 고객사 문의별 질문 수정
    @Transactional
    public QuestionResponseDTO updateInquiryQuestionById(
        String token,
        Long customerId,
        Long inquiryId,
        Long questionId,
        MultipartFile file,
        QuestionUpdateRequestDTO dto
    ) {
        Customer customer = validateCustomer(token);

        Inquiry inquiry = validateInquiry(inquiryId);

        Question question = validateQuestion(questionId);

        validateUserMatch(customer.getUserId(), customerId);

        validateQuestionMatch(question, customerId);

        validateQuestionStatusAndActivated(question);

        validateQuestionCol(question);

        String fileName = question.getFileName();
        String filePath = question.getFilePath();

        boolean isFileDeleted = dto.isFileDeleted() != null && dto.isFileDeleted();

        if (isFileDeleted) {
            fileName = null;
            filePath = null;
        } else if (file != null) {
            FileInfo fileInfo = fileClient.uploadFile(file);
            fileName = fileInfo.getOriginName();
            filePath = fileInfo.getStoredFilePath();
        }

        question.updateQuestion(
            inquiryId,
            dto.title(),
            dto.contents(),
            fileName,
            filePath,
            dto.type(),
            dto.status()
        );

        return QuestionResponseDTO.from(question,userClient,inquiryClient);
    }

    // 고객사 기타 질문 수정
    @Transactional
    public QuestionResponseDTO updateGeneralQuestion(
        String token,
        Long customerId,
        Long questionId,
        MultipartFile file,
        QuestionUpdateRequestDTO dto
    ) {
        Customer customer = validateCustomer(token);

        Question question = validateQuestion(questionId);

        validateUserMatch(customer.getUserId(), customerId);

        validateQuestionMatch(question, customerId);

        validateQuestionStatusAndActivated(question);

        validateQuestionCol(question);

        String fileName = question.getFileName();
        String filePath = question.getFilePath();

        boolean isFileDeleted = dto.isFileDeleted() != null && dto.isFileDeleted();

        if (isFileDeleted) {
            fileName = null;
            filePath = null;
        } else if (file != null) {
            FileInfo fileInfo = fileClient.uploadFile(file);
            fileName = fileInfo.getOriginName();
            filePath = fileInfo.getStoredFilePath();
        }

        question.updateQuestion(
            null,
            dto.title(),
            dto.contents(),
            fileName,
            filePath,
            dto.type(),
            dto.status()
        );

        return QuestionResponseDTO.from(question,userClient,inquiryClient);
    }

    // 질문 삭제 (고객사용)
    @Transactional
    public void deleteQuestionById(
        String token,
        Long customerId,
        Long questionId
    ) {
        Customer customer = validateCustomer(token);

        Question question = validateQuestion(questionId);

        validateUserMatch(customer.getUserId(), customerId);

        validateQuestionMatch(question, customerId);

        validateQuestionStatusAndActivated(question);

        validateQuestionCol(question);

        question.deleteQuestion();
    }

    // 질문 삭제 (담당자용)
    @Transactional
    public void deleteQuestionById(
        String token,
        Long questionId
    ) {
        validateManager(token);

        Question question = validateQuestion(questionId);

        validateQuestionStatusAndActivated(question);

        validateQuestionCol(question);

        question.deleteQuestion();
    }

    // 모바일 전체 문의 조회
    @Transactional(readOnly = true)
    public List<MobileQuestionSummaryResponseDTO> getAllQuestions() {
        return questionRepository.findActiveQuestions().stream()
            .map(question -> MobileQuestionSummaryResponseDTO.from(question, userClient,inquiryClient)) // 람다식을 사용하여 userClient 전달
            .collect(Collectors.toList());
    }

    // 모바일 상세 문의 조회
    @Transactional(readOnly = true)
    public MobileQuestionSummaryResponseDTO getQuestionById(Long questionId) {
        Question question = questionRepository.findActiveQuestionByQuestionId(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND));

        return MobileQuestionSummaryResponseDTO.from(question,userClient,inquiryClient);
    }

    private Inquiry validateInquiry(Long inquiryId) {
        if (inquiryId == null) {
            return null;
        }

        Inquiry inquiry = inquiryClient.getInquiryByIdWithoutToken(inquiryId).getData();
        if(inquiry == null){
            throw new CommonException(ErrorCode.INQUIRY_NOT_FOUND);
        }

        return inquiry;
    }

    private Manager validateManager(String token) {
        Long userId = userClient.parseToken(token);

        Manager manager = userClient.getManagerByIdWithoutToken(userId).getData();
        if(manager == null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        return manager;
    }

    private Customer validateCustomer(String token) {
        Long userId = userClient.parseToken(token);

        Customer customer = userClient.getCustomerByIdWithoutToken(userId).getData();
        if(customer==null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        return customer;
    }

    private Question validateQuestion(Long questionId) {

        return questionRepository.findById(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND));
    }

    private void validateInquiryMatch(Inquiry inquiry, Long customerId) {
        if(!Objects.equals(inquiry.getCustomerId(), customerId))
            throw new CommonException(ErrorCode.INQUIRY_NOT_MATCHED);
    }

    private void validateUserMatch(Long userId, Long customerId) {
        if (!Objects.equals(userId, customerId))
            throw new CommonException(ErrorCode.USER_NOT_MATCHED);
    }

    private void validateQuestionMatch(Question question, Long customerId) {
        if(!Objects.equals(question.getUserId(), customerId))
            throw new CommonException((ErrorCode.QUESTION_NOT_MATCHED));
    }

    private void validateQuestionStatusAndActivated(Question question) {
        if(question.getStatus() == QuestionStatus.COMPLETED)
            throw new CommonException(ErrorCode.QUESTION_STATUS_COMPLETED);

        if(!question.getIsActivated())
            throw new CommonException(ErrorCode.QUESTION_ALREADY_DELETED);
    }

    private void validateQuestionCol(Question question) {
        Collaboration collaboration = collaborationRepository
            .findByQuestionId(question)
            .orElse(null);

        if (collaboration != null) {
            throw new CommonException(ErrorCode.COLLABORATION_STATUS_READY);
        }
    }

    // 모바일 문의 답변 검색
    @Transactional(readOnly = true)
    public List<MobileQuestionSummaryResponseDTO> getQuestionsBySearch(
            String sortBy,
            QuestionStatus status,
            QuestionType type,
            String title,
            String customerName,
            LocalDate startDate,
            LocalDate endDate
    ) {

        List<QuestionSummaryResponseDTO> questions = questionRepository.findQuestionsBySearch(
                sortBy,
                status,
                type,
                title,
                customerName,
                startDate,
                endDate
        );

        return questions.stream()
                .map(MobileQuestionSummaryResponseDTO::toMobileResponseDTO)
                .toList();
    }
}
