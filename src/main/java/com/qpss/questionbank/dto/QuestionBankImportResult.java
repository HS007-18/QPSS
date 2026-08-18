package com.qpss.questionbank.dto;

import com.qpss.questionbank.model.QuestionBankImport;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuestionBankImportResult {
    private QuestionBankImport importBatch;
    private int questionsParsed;
    private int skippedDuplicates;
    private List<String> parsingErrors;
    private boolean successful;
}