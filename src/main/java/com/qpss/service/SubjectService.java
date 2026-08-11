package com.qpss.service;

import com.qpss.model.Session;
import com.qpss.model.Subject;
import com.qpss.model.GeneratedPaper;
import com.qpss.repository.*;
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
        // 1. Find all sessions for this subject
        List<Session> sessions = sessionRepo.findBySubjectId(id);
        if (!sessions.isEmpty()) {
            List<Long> sessionIds = sessions.stream().map(Session::getId).collect(Collectors.toList());
            
            // 2. Delete all questions for this subject
            questionRepo.deleteBySubjectId(id);
            
            // 3. Find and delete all generated papers and their questions
            List<GeneratedPaper> papers = paperRepo.findBySessionIdIn(sessionIds);
            if (!papers.isEmpty()) {
                List<Long> paperIds = papers.stream().map(GeneratedPaper::getId).collect(Collectors.toList());
                paperQuestionRepo.deleteByPaperIdIn(paperIds);
                paperRepo.deleteBySessionIdIn(sessionIds);
            }
            
            // 4. Delete the sessions
            sessionRepo.deleteBySubjectId(id);
        }
        
        // 5. Finally delete the subject
        repo.deleteById(id);
    }
}
