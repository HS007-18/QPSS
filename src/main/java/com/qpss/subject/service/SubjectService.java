package com.qpss.subject.service;

import com.qpss.generation.model.GeneratedPaper;
import com.qpss.generation.repository.GeneratedPaperRepository;
import com.qpss.generation.repository.PaperQuestionRepository;
import com.qpss.questionbank.model.SourceDocument;
import com.qpss.questionbank.repository.QuestionBankImportRepository;
import com.qpss.questionbank.repository.QuestionRepository;
import com.qpss.questionbank.repository.SourceDocumentRepository;
import com.qpss.questionbank.service.SourceDocumentStorageService;
import com.qpss.session.model.Session;
import com.qpss.subject.model.Subject;
import com.qpss.session.repository.SessionRepository;
import com.qpss.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
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