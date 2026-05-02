package com.resumeai.template.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.resumeai.template.entity.Template;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    List<Template> findAll();

    List<Template> findByIsActiveTrue();

    Optional<Template> findByTemplateId(Long templateId);

    List<Template> findByIsPremium(Boolean isPremium);

    List<Template> findByIsActiveTrueAndIsPremium(Boolean isPremium);

    long countByIsActiveTrue();

    boolean existsByName(String name);

    void deleteByTemplateId(Long templateId);
}
