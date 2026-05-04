package com.resumeai.jobmatch.repository;

import com.resumeai.jobmatch.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByTitleContainingIgnoreCase(String keyword);

    void deleteBySource(com.resumeai.jobmatch.entity.JobSource source);

    List<Job> findBySourceOrderByCreatedAtDesc(com.resumeai.jobmatch.entity.JobSource source);
}
