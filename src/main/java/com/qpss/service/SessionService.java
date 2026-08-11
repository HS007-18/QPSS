package com.qpss.service;

import com.qpss.model.Session;
import com.qpss.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository repo;

    public List<Session> findBySubject(Long subjectId) {
        return repo.findBySubjectId(subjectId);
    }

    public Session findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + id));
    }

    public Session create(Long subjectId) {
        return repo.save(Session.builder().subjectId(subjectId).build());
    }

    public Session close(Long id) {
        Session session = findById(id);
        session.setStatus("CLOSED");
        session.setClosedAt(LocalDateTime.now());
        return repo.save(session);
    }

    public List<Session> findActive(Long subjectId) {
        return repo.findBySubjectIdAndStatus(subjectId, "ACTIVE");
    }
}
