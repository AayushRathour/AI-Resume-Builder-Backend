package com.resumeai.jobmatch.service;

import com.resumeai.jobmatch.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Publishes domain events to RabbitMQ for asynchronous processing. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange:resumeai.exchange}")
    private String exchange;

    /**
     * Publishes an ats.completed event
     */
    public void publishAtsCompletedEvent(Long userId, Long resumeId, double atsScore) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .resumeId(resumeId)
                .type("ATS_COMPLETED")
                .title("ATS Score Generated")
                .message(String.format("Your ATS score is %.0f%%. Check your results for improvement suggestions!", atsScore))
                .subject("ATS Score Generated")
                .critical(false)
                .metadataJson("{\"atsScore\":" + atsScore + "}")
                .build();

        publishEvent(event, "ats.completed");
    }

    /**
     * Publishes a job.applied event
     */
    public void publishJobAppliedEvent(Long userId, String jobTitle, String company) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .type("JOB_APPLIED")
                .title("Job Application Submitted")
                .message("Your application for '" + jobTitle + "' at " + company + " has been submitted.")
                .subject("Job Application Submitted")
                .critical(false)
                .build();

        publishEvent(event, "job.applied");
    }

    /**
     * Publishes a job.match event
     */
    public void publishJobMatchEvent(Long userId, int matchCount) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .type("JOB_MATCH")
                .title("New Job Matches Found")
                .message("We found " + matchCount + " new job matches for your resume. Check them out!")
                .subject("New Job Matches Found")
                .critical(false)
                .build();

        publishEvent(event, "ats.completed");
    }

    /**
     * Publishes a job analysis completion event (AI_COMPLETED)
     */
    public void publishAnalysisCompletedEvent(Long userId) {
        publishAnalysisCompletedEvent(userId, null);
    }

    public void publishAnalysisCompletedEvent(Long userId, Long resumeId) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .resumeId(resumeId)
                .type("AI_COMPLETED")
                .title("AI Analysis Complete")
                .message("Your resume analysis is complete. New job matches are available.")
                .subject("AI Analysis Complete")
                .critical(false)
                .build();

        publishEvent(event, "ats.completed");
    }

    /**
     * Generic event publisher
     */
    private void publishEvent(NotificationEvent event, String routingKey) {
        try {
            log.info("[RabbitMQ] Publishing event: exchange='{}', routingKey='{}', type={}, userId={}",
                    exchange, routingKey, event.getType(), event.getUserId());
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("[RabbitMQ] Event published successfully: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish event for userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }
}
