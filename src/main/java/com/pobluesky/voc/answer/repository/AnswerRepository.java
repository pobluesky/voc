package com.pobluesky.voc.answer.repository;

import com.pobluesky.voc.answer.entity.Answer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findAllByCustomerId(Long userId);

    Optional<Answer> findByQuestion_QuestionId(Long questionId);

    @Query(value = "WITH months AS (" +
        "    SELECT generate_series(1, 12) AS month" +
        "), " +
        "counts AS (" +
        "    SELECT EXTRACT(MONTH FROM a.created_date) AS month, " +
        "           COUNT(*) AS answer_count " +
        "    FROM Answer a " +
        "    GROUP BY EXTRACT(MONTH FROM a.created_date)" +
        ") " +
        "SELECT m.month, " +
        "       COALESCE(c.answer_count, 0) AS answer_count " +
        "FROM months m " +
        "LEFT JOIN counts c ON m.month = c.month " +
        "ORDER BY m.month", nativeQuery = true)
    List<Object[]> findAverageCountPerMonth();

    @Query(value = "WITH months AS (" +
        "    SELECT generate_series(1, 12) AS month" +
        "), " +
        "counts AS (" +
        "    SELECT EXTRACT(MONTH FROM a.created_date) AS month, " +
        "           COUNT(*) AS answer_count " +
        "    FROM Answer a " +
        "    WHERE a.manager_id = :managerId " +
        "    GROUP BY EXTRACT(MONTH FROM a.created_date)" +
        ") " +
        "SELECT m.month, " +
        "       COALESCE(c.answer_count, 0) AS answer_count " +
        "FROM months m " +
        "LEFT JOIN counts c ON m.month = c.month " +
        "ORDER BY m.month", nativeQuery = true)
    List<Object[]> findAverageCountPerMonthByManager(@Param("managerId") Long managerId);
}
