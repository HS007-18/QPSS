package com.qpss.service;

import com.qpss.model.Session;
import com.qpss.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository repo;

    public Session findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + id));
    }

    public Session create(Long subjectId) {
        return repo.save(Session.builder().subjectId(subjectId).build());
    }
}