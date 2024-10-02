package com.pobluesky.voc.collaboration.repository;

import com.pobluesky.voc.collaboration.entity.Collaboration;
import com.pobluesky.voc.question.entity.Question;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CollaborationRepository extends JpaRepository<Collaboration, Long>, CollaborationRepositoryCustom {

    @Query("SELECT c FROM Collaboration c WHERE c.colId = :collaborationId AND c.question = :question")
    Optional<Collaboration> findByIdAndQuestion(@Param("collaborationId") Long collaborationId, @Param("question") Question question);

    @Query("SELECT c FROM Collaboration c WHERE c.question = :question")
    Optional<Collaboration> findByQuestionId(@Param("question") Question question);

    @Query(value = "WITH months AS (" +
        "    SELECT generate_series(1, 12) AS month" +
        "), " +
        "counts AS (" +
        "    SELECT EXTRACT(MONTH FROM c.created_date) AS month, " +
        "           COUNT(*) AS col_count " +
        "    FROM Collaboration c " +
        "    GROUP BY EXTRACT(MONTH FROM c.created_date)" +
        ") " +
        "SELECT m.month, " +
        "       COALESCE(c.col_count, 0) AS col_count " +
        "FROM months m " +
        "LEFT JOIN counts c ON m.month = c.month " +
        "ORDER BY m.month", nativeQuery = true)
    List<Object[]> findAverageCountPerMonth();

    @Query(value = "WITH months AS (" +
        "    SELECT generate_series(1, 12) AS month" +
        "), " +
        "counts AS (" +
        "    SELECT EXTRACT(MONTH FROM c.created_date) AS month, " +
        "           COUNT(*) AS col_count " +
        "    FROM Collaboration c " +
        "    WHERE c.col_request_id = :managerId OR c.col_response_id = :managerId" +
        "    GROUP BY EXTRACT(MONTH FROM c.created_date)" +
        ") " +
        "SELECT m.month, " +
        "       COALESCE(c.col_count, 0) AS col_count " +
        "FROM months m " +
        "LEFT JOIN counts c ON m.month = c.month " +
        "ORDER BY m.month", nativeQuery = true)
    List<Object[]> findAverageCountPerMonthByManager(@Param("managerId") Long managerId);
}
