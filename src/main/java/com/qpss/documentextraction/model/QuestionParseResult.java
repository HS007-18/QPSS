package com.qpss.documentextraction.model;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;
@Getter
public class QuestionParseResult {
    private final List<ParsedQuestion> validQuestions = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public void addValidQuestion(ParsedQuestion q) {
        this.validQuestions.add(q);
    }

    public void addError(String error) {
        this.errors.add(error);
    }
}
