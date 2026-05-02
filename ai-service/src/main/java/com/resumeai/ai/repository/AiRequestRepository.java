package com.resumeai.ai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.resumeai.ai.entity.AiRequest;
import com.resumeai.ai.entity.RequestStatus;
import com.resumeai.ai.entity.RequestType;

@Repository
public interface AiRequestRepository extends JpaRepository<AiRequest, String> {

    List<AiRequest> findByUserId(Long userId);

    List<AiRequest> findByResumeId(Long resumeId);

    Optional<AiRequest> findByRequestId(String requestId);

    List<AiRequest> findByRequestType(RequestType requestType);

    List<AiRequest> findByStatus(RequestStatus status);

    long countByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(r.tokensUsed), 0) FROM AiRequest r WHERE r.userId = :userId")
    long sumTokensByUserId(@Param("userId") Long userId);

    // For quota enforcement
    @Query("SELECT COUNT(r) FROM AiRequest r WHERE r.userId = :userId AND r.requestType = :type AND MONTH(r.createdAt) = MONTH(CURRENT_DATE) AND YEAR(r.createdAt) = YEAR(CURRENT_DATE)")
    long countByUserIdAndRequestTypeCurrentMonth(@Param("userId") Long userId, @Param("type") RequestType type);

    @Query("SELECT COUNT(r) FROM AiRequest r WHERE r.userId = :userId AND MONTH(r.createdAt) = MONTH(CURRENT_DATE) AND YEAR(r.createdAt) = YEAR(CURRENT_DATE)")
    long countByUserIdCurrentMonth(@Param("userId") Long userId);
}
