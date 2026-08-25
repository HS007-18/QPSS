package com.qpss.backend.paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
public interface GeneratedPaperRepository extends JpaRepository<GeneratedPaper, Long> {

    List<GeneratedPaper> findByIsFinalTrue();

    List<GeneratedPaper> findBySessionIdIn(List<Long> sessionIds);

    List<GeneratedPaper> findBySessionId(Long sessionId);

    @Modifying
    @Transactional
    void deleteBySessionIdIn(List<Long> sessionIds);
}