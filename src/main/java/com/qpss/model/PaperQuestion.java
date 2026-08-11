package com.qpss.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "paper_questions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaperQuestion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paper_id", nullable = false)
    private Long paperId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(nullable = false)
    private String section;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Column(name = "choice_label")
    private String choiceLabel;

    @Column(name = "pair_index")
    private Integer pairIndex;
}
