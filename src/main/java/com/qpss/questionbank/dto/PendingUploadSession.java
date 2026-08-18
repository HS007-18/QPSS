package com.qpss.questionbank.dto;

import com.qpss.questionbank.parser.QuestionParseResult;
import lombok.Data;


import java.util.List;

@Data
public class PendingUploadSession {
    private Long subjectId;
    private Long sessionId;
    private List<FileImportHolder> files;

    @Data
    public static class FileImportHolder {
        private String originalName;
        private String checksum;
        private QuestionParseResult parseResult;
        private byte[] fileBytes;
        private String contentType;
        private long size;
    }
}