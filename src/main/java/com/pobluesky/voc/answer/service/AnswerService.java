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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
        Long userId = userClient.parseToken(token);

        if(!userClient.managerExists(userId)){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        List<Answer> answers = answerRepository.findAll();

        return answers.stream()
            .map(answer -> AnswerResponseDTO.from(answer,inquiryClient))
            .collect(Collectors.toList());
    }

    // 고객별 답변 전체 조회 (고객사)
    @Transactional(readOnly = true)
    public List<AnswerResponseDTO> getAnswerByUserId(String token, Long customerId) {
        Long userId = userClient.parseToken(token);

        Customer customer = userClient.getCustomerByIdWithoutToken(userId).getData();
        if(customer==null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        if (!customer.getUserId().equals(customerId))
            throw new CommonException(ErrorCode.USER_NOT_MATCHED);

        List<Answer> answers = answerRepository.findAllByCustomerId(customerId);

        if (answers.isEmpty()) {
            throw new CommonException(ErrorCode.ANSWER_NOT_FOUND);
        }

        return answers.stream()
            .map(answer -> AnswerResponseDTO.from(answer,inquiryClient))
            .collect(Collectors.toList());
    }

    // 질문 번호별 답변 상세 조회 (담당자)
    @Transactional(readOnly = true)
    public AnswerResponseDTO getAnswerByQuestionIdForManager(String token, Long questionId) {
        Long userId = userClient.parseToken(token);

        if(!userClient.managerExists(userId)){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        Answer answer = answerRepository.findByQuestion_QuestionId(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.ANSWER_NOT_FOUND));

        return AnswerResponseDTO.from(answer,inquiryClient);
    }

    // 질문 번호별 답변 상세 조회 (고객사)
    @Transactional(readOnly = true)
    public AnswerResponseDTO getAnswerByQuestionId(String token, Long customerId, Long questionId) {
        Long userId = userClient.parseToken(token);

        Customer customer = userClient.getCustomerByIdWithoutToken(userId).getData();
        if(customer==null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        if (!Objects.equals(customer.getUserId(), customerId)) {
            throw new CommonException(ErrorCode.USER_NOT_MATCHED);
        }

        Answer answer = answerRepository.findByQuestion_QuestionId(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.ANSWER_NOT_FOUND));

        if (!Objects.equals(answer.getCustomerId(), customerId)) {
            throw new CommonException(ErrorCode.USER_NOT_MATCHED);
        }

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
        Long userId = userClient.parseToken(token);

        Manager manager = userClient.getManagerByIdWithoutToken(userId).getData();
        if(manager == null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }  // 존재하지 않는 담당자일 경우

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.QUESTION_NOT_FOUND)); // 존재하지 않는 질문인 경우

        if (question.getStatus() == QuestionStatus.COMPLETED) {
            throw new CommonException(ErrorCode.QUESTION_STATUS_COMPLETED); // 이미 답변된 질문인 경우
        }

        Inquiry inquiry = inquiryClient.getInquiryByIdWithoutToken(question.getInquiryId()).getData();
        if(inquiry == null){
            throw new CommonException(ErrorCode.INQUIRY_NOT_FOUND);
        }

        Customer customer = userClient.getCustomerByIdWithoutToken(userId).getData();
        if(customer == null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        String fileName = null;
        String filePath = null;

        if (file != null) {
            FileInfo fileInfo = fileClient.uploadFile(file);
            fileName = fileInfo.getOriginName();
            filePath = fileInfo.getStoredFilePath();
        }

        Answer answer = dto.toAnswerEntity(question, inquiry.getInquiryId(), customer.getUserId(), manager.getUserId(), fileName, filePath);
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
        Long userId = userClient.parseToken(token);

        Manager manager = userClient.getManagerByIdWithoutToken(userId).getData();
        if(manager == null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        Answer answer = answerRepository.findByQuestion_QuestionId(questionId)
            .orElseThrow(() -> new CommonException(ErrorCode.ANSWER_NOT_FOUND));

        if(!Objects.equals(answer.getManagerId(), userId))
            throw new CommonException(ErrorCode.ANSWER_NOT_MATCHED);

        String fileName = answer.getFileName();
        String filePath = answer.getFilePath();

        if (file != null) {
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

    // 답변 삭제
    @Transactional
    public void deleteAnswerById(
        String token,
        Long questionId
    ) {
        Long userId = userClient.parseToken(token);

        Manager manager = userClient.getManagerByIdWithoutToken(userId).getData();
        if(manager == null){
            throw new CommonException(ErrorCode.USER_NOT_FOUND);
        }

        Answer answer = answerRepository.findByQuestion_QuestionId(questionId)
            .orElseThrow(()-> new CommonException(ErrorCode.ANSWER_NOT_FOUND));

        if(!answer.getIsActivated())
            throw new CommonException(ErrorCode.ANSWER_ALREADY_DELETED);

        answer.deleteAnswer();
    }

//    private Inquiry validateInquiry(Question question) {
//        if (question.getInquiry() == null) {
//            return null;
//        }
//
//        return inquiryRepository.findById(question.getInquiry().getInquiryId())
//            .orElseThrow(() -> new CommonException(ErrorCode.INQUIRY_NOT_FOUND));
//    }

    // 모바일 - 질문 번호별 답변 상세 조회
    @Transactional(readOnly = true)
    public MobileAnswerSummaryResponseDTO getAnswerByQuestionId(Long questionId) {
        Answer answer = answerRepository.findByQuestion_QuestionId(questionId)
                .orElseThrow(() -> new CommonException(ErrorCode.ANSWER_NOT_FOUND));

        return MobileAnswerSummaryResponseDTO.from(answer);
    }
}
