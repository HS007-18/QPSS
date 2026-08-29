package com.qpss.service;

import com.qpss.document.model.HeaderMetadata;
import com.qpss.document.parser.HeaderMetadataExtractor;
import com.qpss.entity.SourceDocument;
import com.qpss.repository.SourceDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class DocumentMetadataService {

    private static final Logger log = LoggerFactory.getLogger(DocumentMetadataService.class);

    private final SourceDocumentRepository sourceDocumentRepository;
    private final SourceDocumentStorageService storageService;
    private final HeaderMetadataExtractor metadataExtractor;

    public HeaderMetadata extractMetadata(Long sourceDocumentId) {
        if (sourceDocumentId != null) {
            SourceDocument doc = sourceDocumentRepository.findById(sourceDocumentId).orElse(null);
            if (doc != null) {
                try {
                    byte[] fileBytes = storageService.loadDocument(doc.getStoredFileName());
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
                         XWPFDocument docx = new XWPFDocument(bis)) {
                        return metadataExtractor.extract(docx);
                    }
                } catch (Exception e) {
                    log.warn("Header metadata extraction failed", e);
                }
            }
        }
        return HeaderMetadata.builder().courseOutcomes(new ArrayList<>()).build();
    }
}
