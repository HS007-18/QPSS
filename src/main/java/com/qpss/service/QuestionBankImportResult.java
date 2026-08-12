package com.qpss.service;

import com.qpss.model.QuestionBankImport;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuestionBankImportResult {
    private QuestionBankImport importBatch;
    private int questionsParsed;
    private List<String> parsingErrors;
    private boolean successful;
}
