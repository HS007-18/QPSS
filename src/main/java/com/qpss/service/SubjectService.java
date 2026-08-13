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

        List<Session> sessions = sessionRepo.findBySubjectId(id);
        if (!sessions.isEmpty()) {
            List<Long> sessionIds = sessions.stream().map(Session::getId).collect(Collectors.toList());

            questionRepo.deleteBySubjectId(id);

            List<GeneratedPaper> papers = paperRepo.findBySessionIdIn(sessionIds);
            if (!papers.isEmpty()) {
                List<Long> paperIds = papers.stream().map(GeneratedPaper::getId).collect(Collectors.toList());
                paperQuestionRepo.deleteByPaperIdIn(paperIds);
                paperRepo.deleteBySessionIdIn(sessionIds);
            }

            sessionRepo.deleteBySubjectId(id);
        }

        repo.deleteById(id);
    }
}
