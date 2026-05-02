package com.resumeai.jobmatch.repository;

import com.resumeai.jobmatch.entity.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {

    List<JobMatch> findByUserId(Long userId);

    List<JobMatch> findByResumeId(Long resumeId);

    void deleteByUserIdAndResumeId(Long userId, Long resumeId);
}
