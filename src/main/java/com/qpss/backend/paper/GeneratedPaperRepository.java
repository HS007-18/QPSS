package com.qpss.backend.paper;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeneratedPaperRepository extends JpaRepository<GeneratedPaper, Long> {

    List<GeneratedPaper> findByIsFinalTrue();

    List<GeneratedPaper> findBySessionIdIn(List<Long> sessionIds);

    List<GeneratedPaper> findBySessionId(Long sessionId);

    void deleteBySessionIdIn(List<Long> sessionIds);
}