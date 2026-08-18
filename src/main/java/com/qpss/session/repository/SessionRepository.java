package com.qpss.session.repository;

import com.qpss.session.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findBySubjectId(Long subjectId);
    List<Session> findBySubjectIdAndStatus(Long subjectId, String status);
    void deleteBySubjectId(Long subjectId);
}
