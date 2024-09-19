package com.pobluesky.voc.answer.entity;

import com.pobluesky.voc.feign.Customer;
import com.pobluesky.voc.feign.Inquiry;
import com.pobluesky.voc.global.BaseEntity;
import com.pobluesky.voc.question.entity.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "answer")
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerId; // 답변 번호

    @OneToOne
    @JoinColumn(name = "question_id")
    private Question question; // 질문 번호

    @JoinColumn(name = "inquiry_id")
    private Long inquiryId; // 문의 번호

    @JoinColumn(name = "customer_id")
    private Long customerId; // 고객사 번호

    @JoinColumn(name = "manager_id")
    private Long managerId; // 답변을 입력한 담당자 번호

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String contents;

    @Column(columnDefinition = "TEXT")
    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String filePath;

    private Boolean isActivated;

    @Builder
    private Answer(
        Question question,
        Long inquiryId,
        Long customerId,
        Long managerId,
        String title,
        String contents,
        String fileName,
        String filePath
    ) {
        this.question = question;
        this.inquiryId = inquiryId;
        this.customerId = customerId;
        this.managerId = managerId;
        this.title = title;
        this.contents = contents;
        this.fileName = fileName;
        this.filePath = filePath;
        this.isActivated = true;
    }

    public void updateAnswer(
        String title,
        String contents,
        String fileName,
        String filePath
    ) {
        this.title = title;
        this.contents = contents;
        this.fileName = fileName;
        this.filePath = filePath;
    }

    public void deleteAnswer() { this.isActivated = false; }
}
