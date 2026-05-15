package com.resumeai.resume.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.entity.ResumeStatus;
import com.resumeai.resume.repository.ResumeRepository;

import lombok.RequiredArgsConstructor;

/**
 * Seeds demo resumes for local development and demos.
 */
@Component
@RequiredArgsConstructor
public class ResumeDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ResumeDataSeeder.class);

    private final ResumeRepository resumeRepository;

    /**
     * Inserts demo data if not already present.
     */
    @Override
    public void run(String... args) {
        final String LANG_ENGLISH = "English";

        List<Resume> demoResumes = List.of(
                Resume.builder()
                        .userId(1L)
                        .title("Frontend Developer Portfolio Resume")
                        .targetJobTitle("Frontend Developer")
                        .templateId(1L)
                        .atsScore(78.0)
                        .status(ResumeStatus.COMPLETE)
                        .language(LANG_ENGLISH)
                        .sectionsJson("{\"summary\":\"React developer with 4+ years building responsive products\",\"skills\":[\"React\",\"TypeScript\",\"Tailwind CSS\"],\"experience\":[\"Built reusable design system components\",\"Improved Lighthouse score from 62 to 92\"]}")
                        .isPublic(Boolean.TRUE)
                        .viewCount(42L)
                        .build(),
                Resume.builder()
                        .userId(2L)
                        .title("Java Backend Engineer Resume")
                        .targetJobTitle("Java Backend Engineer")
                        .templateId(4L)
                        .atsScore(84.0)
                        .status(ResumeStatus.COMPLETE)
                        .language(LANG_ENGLISH)
                        .sectionsJson("{\"summary\":\"Backend engineer focused on microservices and scalable APIs\",\"skills\":[\"Java\",\"Spring Boot\",\"MySQL\",\"Docker\"],\"experience\":[\"Designed resilient REST APIs\",\"Reduced API latency by 35 percent\"]}")
                        .isPublic(Boolean.TRUE)
                        .viewCount(31L)
                        .build(),
                    Resume.builder()
                        .userId(3L)
                        .title("Product Manager Resume")
                        .targetJobTitle("Product Manager")
                        .templateId(2L)
                        .atsScore(81.0)
                        .status(ResumeStatus.COMPLETE)
                        .language(LANG_ENGLISH)
                        .sectionsJson("{\"summary\":\"Product manager driving roadmap, analytics, and cross-team execution\",\"skills\":[\"Product Strategy\",\"Roadmapping\",\"SQL\",\"A/B Testing\"],\"experience\":[\"Launched onboarding flow improving activation by 18 percent\",\"Partnered with engineering to ship quarterly roadmap\"]}")
                        .isPublic(Boolean.TRUE)
                        .viewCount(19L)
                        .build(),
                    Resume.builder()
                        .userId(4L)
                        .title("Data Analyst Resume")
                        .targetJobTitle("Data Analyst")
                        .templateId(3L)
                        .atsScore(76.0)
                        .status(ResumeStatus.COMPLETE)
                        .language(LANG_ENGLISH)
                        .sectionsJson("{\"summary\":\"Analyst specializing in dashboards, metrics, and business insights\",\"skills\":[\"SQL\",\"Power BI\",\"Python\",\"Data Modeling\"],\"experience\":[\"Built KPI dashboards for 6 teams\",\"Automated reporting to save 10 hours per week\"]}")
                        .isPublic(Boolean.TRUE)
                        .viewCount(27L)
                        .build());

        int inserted = 0;
        for (Resume resume : demoResumes) {
            if (!resumeRepository.existsByTitle(resume.getTitle())) {
                resumeRepository.save(resume);
                inserted++;
            }
        }

        if (inserted > 0) {
            log.info("Seeded {} public demo resumes", inserted);
        }
    }
}
