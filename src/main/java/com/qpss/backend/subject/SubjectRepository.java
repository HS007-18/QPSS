package com.qpss.backend.subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByCode(String code);

    Optional<Subject> findByCodeIgnoreCase(String code);

    @Query("SELECT s FROM Subject s WHERE LOWER(s.code) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Subject> searchByCodeOrName(@Param("query") String query);
}
