package com.qpss.backend.subject;
import com.qpss.backend.paper.GeneratedPaper;
import com.qpss.backend.paper.GeneratedPaperRepository;
import com.qpss.backend.paper.PaperQuestionRepository;
import com.qpss.backend.questionbank.SourceDocument;
import com.qpss.backend.questionbank.QuestionBankImportRepository;
import com.qpss.backend.questionbank.QuestionRepository;
import com.qpss.backend.questionbank.SourceDocumentRepository;
import com.qpss.backend.questionbank.SourceDocumentStorageService;
import com.qpss.backend.session.Session;
import com.qpss.backend.session.SessionRepository;
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