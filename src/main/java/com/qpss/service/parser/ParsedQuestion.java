package com.qpss.service.parser;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ParsedQuestion {
    private Integer serialNo;
    private String questionContent;
    private Integer marks;
    private String co;
    private Integer t;
    private Integer unit;
    private String rbt;
    private String marksSplit;
    private String questionType;

    public List<QuestionFields> missingFields() {
        List<QuestionFields> missing = new ArrayList<>();
        if (unit == null) {
            missing.add(QuestionFields.UNIT);
        }
        if (isBlank(co)) {
            missing.add(QuestionFields.CO);
        }
        if (marks == null) {
            missing.add(QuestionFields.MARKS);
        }
        if (t == null) {
            missing.add(QuestionFields.T);
        }
        if (isBlank(rbt)) {
            missing.add(QuestionFields.RBT);
        }
        if (isBlank(questionContent)) {
            missing.add(QuestionFields.CONTENT);
        }
        return missing;
    }

    public boolean isComplete() {
        return missingFields().isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}