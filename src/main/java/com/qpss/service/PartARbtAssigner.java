package com.qpss.service;

import com.qpss.model.Question;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PartARbtAssigner {

    private static final String[] CYCLE = {"R", "U", "AP", "AZ"};

    private PartARbtAssigner() {
    }

    public static Map<Long, String> assign(List<Question> twoMarkQuestions) {
        Map<Long, String> rbtByQuestion = new LinkedHashMap<>();
        for (int i = 0; i < twoMarkQuestions.size(); i++) {
            rbtByQuestion.put(twoMarkQuestions.get(i).getId(), CYCLE[i % CYCLE.length]);
        }
        return rbtByQuestion;
    }
}
