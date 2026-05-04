package com.resumeai.jobmatch.repository;

import com.resumeai.jobmatch.entity.JobMatch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {

    // STEP 6: Explicit query methods with @Query annotations
    
    @Query("SELECT j FROM JobMatch j WHERE j.userId = :userId ORDER BY j.matchScore DESC, j.createdAt DESC")
    List<JobMatch> findByUserIdOrderByMatchScoreDescCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT j FROM JobMatch j WHERE j.userId = :userId ORDER BY j.matchScore DESC, j.createdAt DESC")
    List<JobMatch> findTopMatches(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT j FROM JobMatch j WHERE j.resumeId = :resumeId ORDER BY j.matchScore DESC, j.createdAt DESC")
    List<JobMatch> findByResumeId(@Param("resumeId") Long resumeId);

    void deleteByUserIdAndResumeId(Long userId, Long resumeId);
}
