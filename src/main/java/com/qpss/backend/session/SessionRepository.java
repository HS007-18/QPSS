package com.qpss.backend.session;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findBySubjectId(Long subjectId);
    void deleteBySubjectId(Long subjectId);
}
