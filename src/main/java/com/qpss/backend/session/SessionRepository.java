package com.qpss.backend.session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findBySubjectId(Long subjectId);
    @Modifying
    @Transactional
    void deleteBySubjectId(Long subjectId);
}
