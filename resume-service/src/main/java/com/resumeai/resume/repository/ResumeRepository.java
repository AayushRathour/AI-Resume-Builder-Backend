package com.resumeai.resume.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.entity.ResumeStatus;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUserId(Long userId);

    Optional<Resume> findByResumeId(Long resumeId);

    List<Resume> findByStatus(ResumeStatus status);

    List<Resume> findByTargetJobTitle(String targetJobTitle);

    List<Resume> findByIsPublic(Boolean isPublic);

    List<Resume> findByTemplateId(Long templateId);

    long countByIsPublic(Boolean isPublic);

    boolean existsByTitle(String title);

    long countByUserId(Long userId);

    void deleteByResumeId(Long resumeId);
}
