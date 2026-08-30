package com.qpss.service;

import com.qpss.entity.Subject;
import com.qpss.repository.SubjectRepository;
import com.qpss.entity.GeneratedPaper;
import com.qpss.repository.GeneratedPaperRepository;
import com.qpss.repository.PaperQuestionRepository;
import com.qpss.entity.SourceDocument;
import com.qpss.repository.QuestionBankImportRepository;
import com.qpss.repository.QuestionRepository;
import com.qpss.repository.SourceDocumentRepository;
import com.qpss.entity.Session;
import com.qpss.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository repo;
    private final SessionRepository sessionRepo;
    private final QuestionRepository questionRepo;
    private final GeneratedPaperRepository paperRepo;
    private final PaperQuestionRepository paperQuestionRepo;
    private final QuestionBankImportRepository importRepo;
    private final SourceDocumentRepository sourceDocumentRepo;
    private final SourceDocumentStorageService storageService;

    public List<Subject> findAll() {
        return repo.findAll();
    }

    public Subject findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Subject not found: " + id));
    }

    public Subject create(String name) {
        return repo.save(Subject.builder().name(name).build());
    }

    /**
     * Find an existing subject by code, or create a new one with the given code and name.
     * Used during bulk upload to auto-group files by subject.
     */
    @Transactional
    public Subject findOrCreate(String code, String name) {
        if (code != null && !code.isBlank()) {
            Optional<Subject> existing = repo.findByCodeIgnoreCase(code.trim());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return repo.save(Subject.builder()
                .code(code != null ? code.trim().toUpperCase() : null)
                .name(name != null ? name.trim() : "Unknown Subject")
                .build());
    }

    public List<Subject> search(String query) {
        if (query == null || query.isBlank()) {
            return repo.findAll();
        }
        return repo.searchByCodeOrName(query.trim());
    }

    public long getQuestionCount(Long subjectId) {
        return questionRepo.countBySubjectId(subjectId);
    }

    public long getImportCount(Long subjectId) {
        return importRepo.countBySubjectId(subjectId);
    }

    @Transactional
    public void delete(Long id) {
        List<Session> sessions = sessionRepo.findBySubjectId(id);
        if (!sessions.isEmpty()) {
            List<Long> sessionIds = sessions.stream().map(Session::getId).collect(Collectors.toList());

            List<GeneratedPaper> papers = paperRepo.findBySessionIdIn(sessionIds);
            if (!papers.isEmpty()) {
                List<Long> paperIds = papers.stream().map(GeneratedPaper::getId).collect(Collectors.toList());
                paperQuestionRepo.deleteByPaperIdIn(paperIds);
                paperRepo.deleteBySessionIdIn(sessionIds);
            }

            questionRepo.deleteBySubjectId(id);

            List<SourceDocument> docs = sourceDocumentRepo.findByImportBatch_SessionIdIn(sessionIds);
            for (SourceDocument doc : docs) {
                storageService.deleteDocument(doc.getStoredFileName());
            }
            sourceDocumentRepo.deleteByImportBatch_SessionIdIn(sessionIds);
            importRepo.deleteBySessionIdIn(sessionIds);

            sessionRepo.deleteBySubjectId(id);
        }

        repo.deleteById(id);
    }
}