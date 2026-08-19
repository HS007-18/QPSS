package com.qpss.backend.paper;
import com.qpss.backend.questionbank.Question;
import com.qpss.backend.selection.PairingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class PaperDraftPersistenceService {

    private final GeneratedPaperRepository paperRepo;
    private final PaperQuestionRepository paperQuestionRepo;

    @Transactional
    public void deleteDrafts(Long sessionId) {
        List<GeneratedPaper> drafts = paperRepo.findBySessionId(sessionId).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsFinal()))
                .collect(Collectors.toList());
        if (drafts.isEmpty()) {
            return;
        }
        List<Long> draftIds = drafts.stream().map(GeneratedPaper::getId).collect(Collectors.toList());
        paperQuestionRepo.deleteByPaperIdIn(draftIds);
        paperRepo.deleteAll(drafts);
    }

    @Transactional
    public GeneratedPaper savePaper(Long sessionId, Long subjectId, String examType, String label) {
        return paperRepo.save(GeneratedPaper.builder()
                .sessionId(sessionId)
                .subjectId(subjectId)
                .examType(examType)
                .setLabel(label)
                .build());
    }

    @Transactional
    public GeneratedPaper save(GeneratedPaper paper) {
        return paperRepo.save(paper);
    }

    public java.util.Optional<GeneratedPaper> findById(Long paperId) {
        return paperRepo.findById(paperId);
    }

    @Transactional
    public void savePaperQuestions(Long paperId, List<Question> twoMark,
                                   List<PairingEngine.QuestionPair> pairs, Map<Long, String> sectionARbt) {
        for (int i = 0; i < twoMark.size(); i++) {
            paperQuestionRepo.save(PaperQuestion.builder()
                    .paperId(paperId)
                    .questionId(twoMark.get(i).getId())
                    .section("SECTION_A")
                    .questionNumber(i + 1)
                    .displayRbt(sectionARbt.get(twoMark.get(i).getId()))
                    .build());
        }

        for (PairingEngine.QuestionPair pair : pairs) {
            int qNum = 10 + pair.getPairIndex();
            paperQuestionRepo.save(PaperQuestion.builder()
                    .paperId(paperId)
                    .questionId(pair.getChoiceA().getId())
                    .section("SECTION_B")
                    .questionNumber(qNum)
                    .choiceLabel("a")
                    .pairIndex(pair.getPairIndex())
                    .build());
            paperQuestionRepo.save(PaperQuestion.builder()
                    .paperId(paperId)
                    .questionId(pair.getChoiceB().getId())
                    .section("SECTION_B")
                    .questionNumber(qNum)
                    .choiceLabel("b")
                    .pairIndex(pair.getPairIndex())
                    .build());
        }
    }
}
