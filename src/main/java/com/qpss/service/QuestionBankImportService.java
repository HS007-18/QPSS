package com.qpss.service;

import com.qpss.dto.PendingUploadSession;
import com.qpss.dto.QuestionBankImportResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public class QuestionBankImportService {

    private final ImportBatchPreparer batchPreparer;
    private final ImportBatchCommitter batchCommitter;

    public QuestionBankImportService(ImportBatchPreparer batchPreparer, ImportBatchCommitter batchCommitter) {
        this.batchPreparer = batchPreparer;
        this.batchCommitter = batchCommitter;
    }

    public PendingUploadSession prepareImportBatch(Long subjectId, List<MultipartFile> files) {
        return batchPreparer.prepareImportBatch(subjectId, files);
    }

    public QuestionBankImportResult commitImportBatch(PendingUploadSession pendingSession) {
        return batchCommitter.commitImportBatch(pendingSession);
    }
}