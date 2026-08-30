package com.qpss.service;

import com.qpss.entity.Question;
import com.qpss.repository.QuestionRepository;
import com.qpss.exception.QuestionSelectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartAOnlyGenerationService {

    private final QuestionRepository questionRepository;
    private final Random random = new Random();

    /**
     * Generates a single paper set by randomly selecting the requested number of questions
     * for each (unit, topic) combination from the database.
     *
     * @param subjectId        The subject ID
     * @param topicCounts      A map containing "unit_topic" keys (e.g. "1_3" for Unit 1 Topic 3) and count values
     * @return A list of selected Question entities
     */
    public List<Question> generatePartASet(Long subjectId, Map<String, Integer> topicCounts) {
        List<Question> selectedQuestions = new ArrayList<>();
        
        int totalRequested = topicCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (totalRequested != 50) {
            throw new QuestionSelectionException("Total questions requested must be exactly 50, but was: " + totalRequested);
        }

        for (Map.Entry<String, Integer> entry : topicCounts.entrySet()) {
            String[] parts = entry.getKey().split("_");
            if (parts.length != 2) {
                log.warn("Invalid topic key format: {}", entry.getKey());
                continue;
            }
            
            Integer unit = Integer.parseInt(parts[0]);
            Integer topic = Integer.parseInt(parts[1]);
            int requiredCount = entry.getValue();
            
            if (requiredCount <= 0) continue;

            List<Question> allInUnit = questionRepository.findBySubjectIdAndUnitAndMarks(subjectId, unit, 2);
            List<Question> available = new ArrayList<>();
            for (Question q : allInUnit) {
                Integer identifier = q.getTopic() != null ? q.getTopic() : q.getT();
                if (identifier != null && identifier.equals(topic)) {
                    available.add(q);
                }
            }

            if (available.size() < requiredCount) {
                throw new QuestionSelectionException(
                        "Not enough 2-mark questions available for Unit " + unit + ", Topic " + topic + 
                        ". Required: " + requiredCount + ", Available: " + available.size());
            }

            Collections.shuffle(available, random);
            selectedQuestions.addAll(available.subList(0, requiredCount));
        }

        return selectedQuestions;
    }
}
