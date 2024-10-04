package com.pobluesky.voc.answer.service;

import com.pobluesky.voc.answer.dto.request.AnswerCreateRequestDTO;
import com.pobluesky.voc.answer.dto.request.AnswerUpdateRequestDTO;
import com.pobluesky.voc.answer.dto.response.AnswerResponseDTO;
import com.pobluesky.voc.answer.dto.response.MobileAnswerSummaryResponseDTO;
import com.pobluesky.voc.answer.entity.Answer;
import com.pobluesky.voc.answer.repository.AnswerRepository;
import com.pobluesky.voc.feign.Customer;
import com.pobluesky.voc.feign.FileClient;
import com.pobluesky.voc.feign.FileInfo;
import com.pobluesky.voc.feign.Inquiry;
import com.pobluesky.voc.feign.InquiryClient;
import com.pobluesky.voc.feign.Manager;
import com.pobluesky.voc.feign.UserClient;
import com.pobluesky.voc.global.error.CommonException;
import com.pobluesky.voc.global.error.ErrorCode;
import com.pobluesky.voc.question.entity.Question;
import com.pobluesky.voc.question.entity.QuestionStatus;
import com.pobluesky.voc.question.repository.QuestionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final QuestionRepository questionRepository;

    private final AnswerRepository answerRepository;

    private final InquiryClient inquiryClient;

    private final UserClient userClient;

    private final FileClient fileClient;

    // 답변 전체 조회 (담당자)
    public List<AnswerResponseDTO> getAnswers(String token) {
        validateManager(token);

        List<Answer> answers = answerRepository.findAll();

        return answers.stream()
            .map(answer -> AnswerResponseDTO.from(answer,inquiryClient))
            .collect(Collectors.toList());
    }

    // 고객별 답변 전체 조회 (고객사)
    @Transactional(readOnly = true)
    public List<AnswerResponseDTO> getAnswerByUserId(String token, Long customerId) {
        Customer customer = validateCustomer(token);

        validateUserMatch(customer.getUserId(), customerId);

        List<Answer> answers = answerRepository.findAllByCustomerId(customerId);

        return answers.stream()
            .map(answer -> AnswerResponseDTO.from(answer,inquiryClient))
            .collect(Collectors.toList());
    }

    // 질문 번호별 답변 상세 조회 (담당자)
    @Transactional(readOnly = true)
    public AnswerResponseDTO getAnswerByQuestionIdForManager(String token, Long questionId) {
        validateManager(token);

        Answer answer = validateAnswer(questionId);

        return AnswerResponseDTO.from(answer,inquiryClient);
    }

    // 질문 번호별 답변 상세 조회 (고객사)
    @Transactional(readOnly = true)
    public AnswerResponseDTO getAnswerByQuestionId(String token, Long customerId, Long questionId) {
        Customer customer = validateCustomer(token);

        Answer answer = validateAnswer(questionId);

        validateUserMatch(customer.getUserId(), customerId);

        validateUserMatch(answer.getCustomerId(), customerId);

        return AnswerResponseDTO.from(answer,inquiryClient);
    }

    // 질문별 답변 작성 (담당자)
    @Transactional
    public AnswerResponseDTO createAnswer(
        String token,
        Long questionId,
        MultipartFile file,
        AnswerCreateRequestDTO dto
    ) {
        Manager manager = validateManager(token);

        Question question = validateAndRetrieveQuestion(questionId);

        Long inquiryId = validateInquiry(question);

        Customer customer = validateAndRetrieveCustomer(question);

        validateQuestionStatus(question);

        String fileName = null;
        String filePath = null;

        if (file != null) {
            FileInfo fileInfo = fileClient.uploadFile(file);
            fileName = fileInfo.getOriginName();
            filePath = fileInfo.getStoredFilePath();
        }

        Answer answer = dto.toAnswerEntity(question, inquiryId, customer.getUserId(), manager.getUserId(), fileName, filePath);
        Answer savedAnswer = answerRepository.save(answer);

        question.setStatus(QuestionStatus.COMPLETED);
        questionRepository.save(question);

        return AnswerResponseDTO.from(savedAnswer,inquiryClient);
    }

    // 답변 수정
    @Transactional
    public AnswerResponseDTO updateAnswerById(
        String token,
        Long questionId,
        MultipartFile file,
        AnswerUpdateRequestDTO dto
    ) {
        Manager manager = validateManager(token);

        Answer answer = validateAnswer(questionId);

        validateAnswerMatch(answer, manager);

        validateAnswerActivated(answer);

        String fileName = answer.getFileName();
        String filePath = answer.getFilePath();

        boolean isFileDeleted = dto.isFileDeleted() != null && dto.isFileDeleted();

        if (isFileDeleted) {
            fileName = null;
            filePath = null;
        } else if (file != null) {
            FileInfo fileInfo = fileClient.uploadFile(file);
            fileName = fileInfo.getOriginName();
            filePath = fileInfo.getStoredFilePath();
        }

        answer.updateAnswer(
            dto.title(),
            dto.contents(),
            fileName,
            filePath
        );

        return AnswerResponseDTO.from(answer,inquiryClient);
    }

    // 월별 담당자별 VoC 답변 건수
    @Transactional(readOnly = true)
    public Map<String, List<Object[]>> getAverageCountPerMonth(String token) {
        Manager manager = validateManager(token);
        Map<String, List<Object[]>> results = new HashMap<>();

        results.put("total", answerRepository.findAverageCountPerMonth());
        results.put("manager", answerRepository.findAverageCountPerMonthByManager(manager.getUserId()));

        return results;
    }

    // 모바일 - 질문 번호별 답변 상세 조회
    @Transactional(readOnly = true)
    public MobileAnswerSummaryResponseDTO getAnswerByQuestionId(Long questionId) {
        Answer answer = answerRepository.findByQuestion_QuestionId(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.ANSWER_NOT_FOUND));

        return MobileAnswerSummaryResponseDTO.from(answer);
    }

    private Long validateInquiry(Question question) {

        if (question.getInquiryId() == null) {
            return null;
        }

        Inquiry inquiry = inquiryClient.getInquiryByIdWithoutToken(question.getInquiryId()).getData();
        if(inquiry == null){
            throw new CommonException(ErrorCode.INQUIRY_NOT_FOUND);
        }

        return inquiry.getInquiryId();
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

    private Customer validateAndRetrieveCustomer(Question question) {

        Customer customer = userClient.getCustomerByIdWithoutToken(question.getUserId()).getData();

        if(customer==null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        return customer;
    }

    private Answer validateAnswer(Long questionId) {

        return answerRepository.findByQuestion_QuestionId(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.ANSWER_NOT_FOUND));
    }

    private Question validateAndRetrieveQuestion(Long questionId) {

        return questionRepository.findById(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND));
    }

    private void validateUserMatch(Long userId, Long customerId) {
        if(!Objects.equals(userId, customerId))
            throw new CommonException(ErrorCode.USER_NOT_MATCHED);
    }

    private void validateAnswerMatch(Answer answer, Manager manager) {
        if(!Objects.equals(answer.getManagerId(), manager.getUserId()))
            throw new CommonException(ErrorCode.ANSWER_NOT_MATCHED);
    }

    private void validateAnswerActivated(Answer answer) {
        if(!answer.getIsActivated())
            throw new CommonException(ErrorCode.ANSWER_ALREADY_DELETED);
    }

    private void validateQuestionStatus(Question question) {
        if (question.getStatus() == QuestionStatus.COMPLETED) {
            throw new CommonException(ErrorCode.QUESTION_STATUS_COMPLETED);
        }
    }
}
