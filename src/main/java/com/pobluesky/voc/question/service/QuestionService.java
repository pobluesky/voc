package com.pobluesky.voc.question.service;

import com.pobluesky.voc.feign.Customer;
import com.pobluesky.voc.feign.FileClient;
import com.pobluesky.voc.feign.FileInfo;
import com.pobluesky.voc.feign.Inquiry;
import com.pobluesky.voc.feign.InquiryClient;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);
    private final QuestionRepository questionRepository;

    private final InquiryClient inquiryClient;

    private final UserClient userClient;

    private final FileClient fileClient;

    // 질문 전체 조회 (담당자) without paging
    @Transactional(readOnly = true)
    public List<QuestionSummaryResponseDTO> getAllQuestionsByManagerWithoutPaging(
        String token,
        String sortBy,
        QuestionStatus status,
        QuestionType type,
        String title,
        Long questionId,
        String customerName,
        LocalDate startDate,
        LocalDate endDate
        ) {

        Long userId = userClient.parseToken(token);

        if(!userClient.managerExists(userId)){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        return questionRepository.findAllQuestionsByManagerWithoutPaging(
            status,
            type,
            title,
            questionId,
            customerName,
            startDate,
            endDate,
            sortBy
            );
    }

    // 질문 전체 조회 (고객사) without paging
    @Transactional(readOnly = true)
    public List<QuestionSummaryResponseDTO> getAllQuestionsByCustomerWithoutPaging(
        String token,
        Long customerId,
        String sortBy,
        QuestionStatus status,
        QuestionType type,
        String title,
        Long questionId,
        LocalDate startDate,
        LocalDate endDate) {

        Long userId = userClient.parseToken(token);

        Customer customer = userClient.getCustomerByIdWithoutToken(userId).getData();
        if(customer == null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        if (!Objects.equals(customer.getUserId(), customerId)) {
            throw new CommonException(ErrorCode.USER_NOT_MATCHED);
        }

        return questionRepository.findAllQuestionsByCustomerWithoutPaging(
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
        Long userId = userClient.parseToken(token);

        if(!userClient.managerExists(userId)){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }


        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND));

        return QuestionResponseDTO.from(question,userClient,inquiryClient);
    }

    // 질문 번호별 질문 조회 (고객사)
    @Transactional(readOnly = true)
    public QuestionResponseDTO getQuestionByQuestionId(String token, Long customerId, Long questionId) {
        Long userId = userClient.parseToken(token);

        Customer customer = userClient.getCustomerByIdWithoutToken(userId).getData();
        if(customer == null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        if (!Objects.equals(customer.getUserId(), customerId)) {
            throw new CommonException(ErrorCode.USER_NOT_MATCHED);
        }

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND));

        if (!Objects.equals(question.getUserId(), customerId)) {
            throw new CommonException(ErrorCode.USER_NOT_MATCHED);
        }

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
        Long userId = userClient.parseToken(token);

        Customer customer = userClient.getCustomerByIdWithoutToken(userId).getData();
        if(customer == null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        if(!Objects.equals(customer.getUserId(), customerId))
            throw new CommonException(ErrorCode.USER_NOT_MATCHED);

        Inquiry inquiry = inquiryClient.getInquiryByIdWithoutToken(inquiryId).getData();
        if(inquiry == null){
            throw new CommonException(ErrorCode.INQUIRY_NOT_FOUND);
        }

        if(!Objects.equals(inquiry.getCustomerId(), customerId))
            throw new CommonException(ErrorCode.INQUIRY_NOT_MATCHED);

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
    public QuestionResponseDTO createNotInquiryQuestion(
        String token,
        Long customerId,
        MultipartFile file,
        QuestionCreateRequestDTO dto
        ) {
        Long userId = userClient.parseToken(token);

        Customer customer = userClient.getCustomerByIdWithoutToken(userId).getData();
        if(customer == null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        if(!Objects.equals(customer.getUserId(), customerId))
            throw new CommonException(ErrorCode.USER_NOT_MATCHED);

        String fileName = null;
        String filePath = null;

        if (file != null) {
            FileInfo fileInfo = fileClient.uploadFile(file);
            fileName = fileInfo.getOriginName();
            filePath = fileInfo.getStoredFilePath();
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
        Long userId = userClient.parseToken(token);

        Customer customer = userClient.getCustomerByIdWithoutToken(userId).getData();
        if(customer == null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        Inquiry inquiry = inquiryClient.getInquiryByIdWithoutToken(inquiryId).getData();
        if(inquiry == null){
            throw new CommonException(ErrorCode.INQUIRY_NOT_FOUND);
        }

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND));

        if(!Objects.equals(question.getUserId(), customerId))
            throw new CommonException((ErrorCode.QUESTION_NOT_MATCHED));

        if(question.getStatus() == QuestionStatus.COMPLETED)
            throw new CommonException(ErrorCode.QUESTION_STATUS_COMPLETED);

        if(!Objects.equals(customer.getUserId(), customerId))
            throw new CommonException(ErrorCode.USER_NOT_MATCHED);

        String fileName = question.getFileName();
        String filePath = question.getFilePath();

        if (file != null) {
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
    public QuestionResponseDTO updateNotInquiryQuestionById(
        String token,
        Long customerId,
        Long questionId,
        MultipartFile file,
        QuestionUpdateRequestDTO dto
    ) {
        Long userId = userClient.parseToken(token);

        Customer customer = userClient.getCustomerByIdWithoutToken(userId).getData();
        if(customer == null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        if(!Objects.equals(customer.getUserId(), customerId))
            throw new CommonException(ErrorCode.USER_NOT_MATCHED);

        Inquiry inquiry = null;

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND));

        if(!Objects.equals(question.getUserId(), customerId))
            throw new CommonException((ErrorCode.QUESTION_NOT_MATCHED));

        if(question.getStatus() == QuestionStatus.COMPLETED)
            throw new CommonException(ErrorCode.QUESTION_STATUS_COMPLETED);

        String fileName = question.getFileName();
        String filePath = question.getFilePath();

        if (file != null) {
            FileInfo fileInfo = fileClient.uploadFile(file);
            fileName = fileInfo.getOriginName();
            filePath = fileInfo.getStoredFilePath();
        }

        question.updateQuestion(
            inquiry.getInquiryId(),
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
        Long userId = userClient.parseToken(token);

        Customer customer = userClient.getCustomerByIdWithoutToken(userId).getData();
        if(customer == null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        if(!Objects.equals(customer.getUserId(), customerId))
            throw new CommonException(ErrorCode.USER_NOT_MATCHED);

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND));

        if(!Objects.equals(question.getUserId(), customerId))
            throw new CommonException((ErrorCode.QUESTION_NOT_MATCHED));

        if(question.getStatus() == QuestionStatus.COMPLETED)
            throw new CommonException(ErrorCode.QUESTION_STATUS_COMPLETED);

        if(!question.getIsActivated())
            throw new CommonException(ErrorCode.QUESTION_ALREADY_DELETED);

        question.deleteQuestion();
    }

    // 질문 삭제 (담당자용)
    @Transactional
    public void deleteQuestionById(
        String token,
        Long questionId
    ) {
        Long userId = userClient.parseToken(token);

        if(!userClient.managerExists(userId)){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND));

        if(question.getStatus() == QuestionStatus.COMPLETED)
            throw new CommonException(ErrorCode.QUESTION_STATUS_COMPLETED);

        if(!question.getIsActivated())
            throw new CommonException(ErrorCode.QUESTION_ALREADY_DELETED);

        question.deleteQuestion();
    }

    // 모바일 전체 문의 조회
    public List<MobileQuestionSummaryResponseDTO> getAllQuestions() {
        return questionRepository.findActiveQuestions().stream()
            .map(question -> MobileQuestionSummaryResponseDTO.from(question, userClient)) // 람다식을 사용하여 userClient 전달
            .collect(Collectors.toList());
    }

    // 모바일 상세 문의 조회
    @Transactional(readOnly = true)
    public MobileQuestionSummaryResponseDTO getQuestionById(Long questionId) {
        Question question = questionRepository.findActiveQuestionByQuestionId(questionId)
                .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND));

        return MobileQuestionSummaryResponseDTO.from(question,userClient);
    }
}
