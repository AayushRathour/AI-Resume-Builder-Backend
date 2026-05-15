package com.resumeai.export.service.impl;

import com.resumeai.export.client.ResumeClient;
import com.resumeai.export.client.SectionClient;
import com.resumeai.export.client.TemplateClient;
import com.resumeai.export.dto.*;
import com.resumeai.export.entity.ExportJobRecord;
import com.resumeai.export.repository.ExportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

    @Mock private ResumeClient resumeClient;
    @Mock private SectionClient sectionClient;
    @Mock private TemplateClient templateClient;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private ExportJobRepository exportJobRepository;

    @InjectMocks
    private ExportServiceImpl exportService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exportService, "exchange", "resumeai.exchange");
        ReflectionTestUtils.setField(exportService, "exportRoutingKey", "resume.exported");
        ReflectionTestUtils.setField(exportService, "outputDir", tempDir.toString());
    }

    private void mockSaveJob() {
        when(exportJobRepository.save(any())).thenAnswer(inv -> {
            ExportJobRecord j = inv.getArgument(0);
            if (j.getJobId() == null) { j.setJobId(UUID.randomUUID()); j.setRequestedAt(LocalDateTime.now()); }
            return j;
        });
    }

    @Test
    void exportResume_resumeNotFound_returnsFailed() {
        mockSaveJob();
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(null);
        ExportResponse r = exportService.exportResume(1L, 100L, "PDF", null);
        assertEquals("FAILED", r.getStatus());
    }

    @Test
    void exportResume_jsonFormat_succeeds() {
        mockSaveJob();
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(
                ResumeDTO.builder().resumeId(100L).userId(1L).title("R").name("J").build());
        when(sectionClient.getSectionsByResumeId(100L, 1L)).thenReturn(List.of(
                SectionDTO.builder().sectionType("SUMMARY").title("Summary").content("Experienced developer").orderIndex(1).isVisible(true).build()));
        assertEquals("COMPLETED", exportService.exportResume(1L, 100L, "JSON", null).getStatus());
    }

    @Test
    void exportResume_withSectionsJson() {
        mockSaveJob();
        String sj = "{\"personal\":{\"name\":\"Jane\"},\"summary\":\"Dev\",\"skills\":[\"Java\"],\"experience\":[{\"position\":\"Dev\",\"company\":\"C\",\"startDate\":\"2020\",\"endDate\":\"2023\",\"description\":\"Led\"}],\"education\":[{\"degree\":\"BS\",\"field\":\"CS\",\"institution\":\"MIT\",\"startDate\":\"2016\",\"endDate\":\"2020\"}],\"projects\":[{\"name\":\"P\",\"technologies\":\"Java\",\"description\":\"D\"}]}";
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(
                ResumeDTO.builder().resumeId(100L).userId(1L).sectionsJson(sj).build());
        when(templateClient.getTemplateById(1L)).thenReturn(
                TemplateDTO.builder().templateId(1L).htmlContent("<div>{{name}}{{summary}}{{skills}}{{experience}}{{education}}{{projects}}</div>").cssContent("body{}").build());
        assertNotNull(exportService.exportResume(1L, 100L, "PDF", 1L));
    }

    @Test
    void exportResume_withEmbeddedFields() {
        mockSaveJob();
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(
                ResumeDTO.builder().resumeId(100L).userId(1L).name("J").title("E").email("j@t.com").phone("555").location("NY").summary("Great").skills("[\"Java\"]").experience("[{\"position\":\"D\",\"company\":\"C\",\"startDate\":\"2020\",\"endDate\":\"2023\",\"description\":\"W\"}]").education("[{\"degree\":\"BS\",\"field\":\"CS\",\"institution\":\"MIT\",\"startDate\":\"2016\",\"endDate\":\"2020\"}]").projects("[{\"name\":\"P\",\"technologies\":\"Java\",\"description\":\"D\"}]").build());
        when(sectionClient.getSectionsByResumeId(100L, 1L)).thenReturn(List.of(
                SectionDTO.builder().sectionType("SUMMARY").title("Summary").content("Great").orderIndex(1).isVisible(true).build()));
        assertEquals("COMPLETED", exportService.exportResume(1L, 100L, "JSON", null).getStatus());
    }

    @Test
    void exportResume_normalizeFormatDefaults() {
        mockSaveJob();
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(null);
        exportService.exportResume(1L, 100L, "INVALID", null);
        ArgumentCaptor<ExportJobRecord> c = ArgumentCaptor.forClass(ExportJobRecord.class);
        verify(exportJobRepository, atLeastOnce()).save(c.capture());
        assertTrue(c.getAllValues().stream().anyMatch(j -> "PDF".equals(j.getFormat())));
    }

    @Test
    void exportResume_nullFormat() {
        mockSaveJob();
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(null);
        exportService.exportResume(1L, 100L, null, null);
        ArgumentCaptor<ExportJobRecord> c = ArgumentCaptor.forClass(ExportJobRecord.class);
        verify(exportJobRepository, atLeastOnce()).save(c.capture());
        assertTrue(c.getAllValues().stream().anyMatch(j -> "PDF".equals(j.getFormat())));
    }

    @Test
    void exportResume_docxFormat() {
        mockSaveJob();
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(null);
        exportService.exportResume(1L, 100L, "docx", null);
        ArgumentCaptor<ExportJobRecord> c = ArgumentCaptor.forClass(ExportJobRecord.class);
        verify(exportJobRepository, atLeastOnce()).save(c.capture());
        assertTrue(c.getAllValues().stream().anyMatch(j -> "DOCX".equals(j.getFormat())));
    }

    @Test
    void exportResume_sectionClientThrows() {
        mockSaveJob();
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(ResumeDTO.builder().resumeId(100L).userId(1L).title("R").name("J").build());
        when(sectionClient.getSectionsByResumeId(100L, 1L)).thenThrow(new RuntimeException("Down"));
        assertNotNull(exportService.exportResume(1L, 100L, "JSON", null));
    }

    @Test
    void exportResume_rabbitFails_stillCompletes() {
        mockSaveJob();
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(ResumeDTO.builder().resumeId(100L).userId(1L).name("J").build());
        when(sectionClient.getSectionsByResumeId(100L, 1L)).thenReturn(List.of(
                SectionDTO.builder().sectionType("SUMMARY").title("Summary").content("Profile").orderIndex(1).isVisible(true).build()));
        doThrow(new RuntimeException("RMQ")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
        assertEquals("COMPLETED", exportService.exportResume(1L, 100L, "JSON", null).getStatus());
    }

    @Test
    void exportResume_sanitizedSummary() {
        mockSaveJob();
        String sj = "{\"personal\":{\"name\":\"T\"},\"summary\":\"DROP TABLE users\"}";
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(ResumeDTO.builder().resumeId(100L).userId(1L).sectionsJson(sj).build());
        assertNotNull(exportService.exportResume(1L, 100L, "JSON", null));
    }

    @Test
    void exportResume_withLinks() {
        mockSaveJob();
        String sj = "{\"personal\":{\"name\":\"T\",\"linkedin\":\"https://linkedin.com/in/t\",\"github\":\"https://github.com/t\",\"website\":\"https://t.com\"}}";
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(ResumeDTO.builder().resumeId(100L).userId(1L).sectionsJson(sj).build());
        assertNotNull(exportService.exportResume(1L, 100L, "JSON", null));
    }

    @Test
    void exportResume_sidebarTemplate() {
        mockSaveJob();
        String sj = "{\"personal\":{\"name\":\"T\"},\"summary\":\"D\"}";
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(ResumeDTO.builder().resumeId(100L).userId(1L).sectionsJson(sj).build());
        when(templateClient.getTemplateById(1L)).thenReturn(TemplateDTO.builder().templateId(1L).htmlContent("<div class=\"page\"><div class=\"sidebar\">{{name}}</div><div class=\"main\">{{summary}}</div></div>").cssContent("body{}").build());
        assertNotNull(exportService.exportResume(1L, 100L, "PDF", 1L));
    }

    @Test
    void exportResume_legacySections() {
        mockSaveJob();
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(ResumeDTO.builder().resumeId(100L).userId(1L).title("R").targetJobTitle("Dev").build());
        when(sectionClient.getSectionsByResumeId(100L, 1L)).thenReturn(List.of(
                SectionDTO.builder().sectionType("EXP").title("Experience").content("5yr").orderIndex(1).isVisible(true).build(),
                SectionDTO.builder().sectionType("HIDDEN").content("Secret").orderIndex(2).isVisible(false).build()));
        assertNotNull(exportService.exportResume(1L, 100L, "PDF", null));
    }

    @Test
    void exportResume_currentExperience() {
        mockSaveJob();
        String sj = "{\"personal\":{\"name\":\"T\"},\"experience\":[{\"position\":\"D\",\"company\":\"C\",\"startDate\":\"2023\",\"current\":true,\"description\":\"W\"}],\"projects\":[{\"name\":\"P\",\"link\":\"https://github.com/t\",\"technologies\":\"J\",\"description\":\"D\"}]}";
        when(resumeClient.getResumeById(100L, 1L)).thenReturn(ResumeDTO.builder().resumeId(100L).userId(1L).sectionsJson(sj).build());
        assertNotNull(exportService.exportResume(1L, 100L, "JSON", null));
    }

    @Test
    void getStatus_found() {
        UUID id = UUID.randomUUID();
        when(exportJobRepository.findById(id)).thenReturn(Optional.of(ExportJobRecord.builder().jobId(id).userId(1L).resumeId(100L).format("PDF").status("COMPLETED").requestedAt(LocalDateTime.now()).build()));
        assertEquals("COMPLETED", exportService.getStatus(id).getStatus());
    }

    @Test
    void getStatus_notFound() {
        UUID id = UUID.randomUUID();
        when(exportJobRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> exportService.getStatus(id));
    }

    @Test
    void getByUser_returnsAll() {
        when(exportJobRepository.findByUserIdOrderByRequestedAtDesc(1L)).thenReturn(List.of(
                ExportJobRecord.builder().jobId(UUID.randomUUID()).userId(1L).resumeId(100L).format("PDF").status("COMPLETED").requestedAt(LocalDateTime.now()).build()));
        assertEquals(1, exportService.getByUser(1L).size());
    }

    @Test
    void getByUser_empty() {
        when(exportJobRepository.findByUserIdOrderByRequestedAtDesc(1L)).thenReturn(List.of());
        assertTrue(exportService.getByUser(1L).isEmpty());
    }

    @Test
    void getStats_correctCounts() {
        when(exportJobRepository.countByUserId(1L)).thenReturn(10L);
        when(exportJobRepository.countByUserIdAndStatus(1L, "COMPLETED")).thenReturn(7L);
        when(exportJobRepository.countByUserIdAndStatus(1L, "FAILED")).thenReturn(2L);
        when(exportJobRepository.countByUserIdAndStatus(1L, "PROCESSING")).thenReturn(1L);
        Map<String, Object> r = exportService.getStats(1L);
        assertEquals(10L, r.get("totalExports"));
        assertEquals(7L, r.get("completedExports"));
    }

    @Test
    void delete_found() {
        UUID id = UUID.randomUUID();
        when(exportJobRepository.findById(id)).thenReturn(Optional.of(ExportJobRecord.builder().jobId(id).userId(1L).resumeId(100L).format("PDF").status("COMPLETED").filePath(null).requestedAt(LocalDateTime.now()).build()));
        exportService.delete(id);
        verify(exportJobRepository).deleteById(id);
    }

    @Test
    void delete_notFound() {
        UUID id = UUID.randomUUID();
        when(exportJobRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> exportService.delete(id));
    }
}
