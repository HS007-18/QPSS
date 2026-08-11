package com.qpss.repository;

import com.qpss.model.GeneratedPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GeneratedPaperRepository extends JpaRepository<GeneratedPaper, Long> {
    void deleteBySessionIdIn(List<Long> sessionIds);
    List<GeneratedPaper> findBySessionIdIn(List<Long> sessionIds);
}
