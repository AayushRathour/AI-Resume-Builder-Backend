package com.resumeai.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.ai.dto.AIResponse;
import com.resumeai.ai.dto.ATSRequest;
import com.resumeai.ai.dto.ATSResponse;
import com.resumeai.ai.dto.BulletRequest;
import com.resumeai.ai.dto.ChatRequest;
import com.resumeai.ai.dto.CoverLetterRequest;
import com.resumeai.ai.dto.ImproveRequest;
import com.resumeai.ai.dto.QuotaResponse;
import com.resumeai.ai.dto.SkillRequest;
import com.resumeai.ai.dto.SummaryRequest;
import com.resumeai.ai.dto.TailorRequest;
import com.resumeai.ai.dto.TranslateRequest;
import com.resumeai.ai.service.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiService aiService;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void generateSummary() throws Exception {
        when(aiService.generateSummary(anyLong(), any(), any())).thenReturn(new AIResponse());
        mockMvc.perform(post("/ai/summary")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SummaryRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void generateBullets() throws Exception {
        when(aiService.generateBulletPoints(anyLong(), any(), any())).thenReturn(new AIResponse());
        mockMvc.perform(post("/ai/bullets")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BulletRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void generateCoverLetter() throws Exception {
        when(aiService.generateCoverLetter(anyLong(), any(), any())).thenReturn(new AIResponse());
        mockMvc.perform(post("/ai/cover-letter")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CoverLetterRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void improveSection() throws Exception {
        when(aiService.improveSection(anyLong(), any(), any())).thenReturn(new AIResponse());
        mockMvc.perform(post("/ai/improve")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ImproveRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void checkAts() throws Exception {
        when(aiService.checkAtsCompatibility(anyLong(), any(), any())).thenReturn(new ATSResponse());
        mockMvc.perform(post("/ai/ats")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ATSRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void suggestSkills() throws Exception {
        when(aiService.suggestSkills(anyLong(), any(), any())).thenReturn(new AIResponse());
        mockMvc.perform(post("/ai/skills")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SkillRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void tailorResume() throws Exception {
        when(aiService.tailorResumeForJob(anyLong(), any(), any())).thenReturn(new AIResponse());
        mockMvc.perform(post("/ai/tailor")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TailorRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void translateResume() throws Exception {
        when(aiService.translateResume(anyLong(), any(), any())).thenReturn(new AIResponse());
        mockMvc.perform(post("/ai/translate")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TranslateRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void chat() throws Exception {
        when(aiService.chat(anyLong(), any())).thenReturn(new AIResponse());
        mockMvc.perform(post("/ai/chat")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChatRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void getHistory() throws Exception {
        when(aiService.getAiHistory(anyLong())).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/ai/history/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getQuota() throws Exception {
        when(aiService.getRemainingQuota(anyLong())).thenReturn(new QuotaResponse());
        mockMvc.perform(get("/ai/quota/1"))
                .andExpect(status().isOk());
    }

    @Test
    void atsUpload() throws Exception {
        when(aiService.checkAtsCompatibility(anyLong(), any(), any())).thenReturn(new ATSResponse());
        // Valid PDF header
        byte[] pdfContent = "%PDF-1.5\n%\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF".getBytes();
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile("file", "test.pdf", "application/pdf", pdfContent);
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/ai/ats-upload")
                .file(file)
                .param("userId", "1"))
                .andExpect(status().isBadRequest()); // fake PDF can't be parsed by PDFBox
    }

    @Test
    void extractResumeFromFile() throws Exception {
        when(aiService.extractResumeData(any())).thenReturn(new com.resumeai.ai.dto.ResumeExtractResponse());
        // Valid TXT content
        byte[] txtContent = "Resume content text".getBytes();
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile("file", "test.txt", "text/plain", txtContent);
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/ai/resume-extract")
                .file(file)
                .param("userId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void extractResume_json() throws Exception {
        when(aiService.extractResumeData(any())).thenReturn(new com.resumeai.ai.dto.ResumeExtractResponse());
        com.resumeai.ai.dto.ResumeExtractRequest req = new com.resumeai.ai.dto.ResumeExtractRequest();
        req.setResumeText("some text");
        
        mockMvc.perform(post("/ai/resume-extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void extractResume_docx() throws Exception {
        when(aiService.extractResumeData(any())).thenReturn(new com.resumeai.ai.dto.ResumeExtractResponse());
        // DOCX header (ZIP)
        byte[] docxContent = "PK\u0003\u0004 content".getBytes();
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile("file", "test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxContent);
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/ai/resume-extract")
                .file(file)
                .param("userId", "1"))
                .andExpect(status().isInternalServerError()); // fake DOCX can't be parsed by POI
    }

    @Test
    void extractResume_emptyFile() throws Exception {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile("file", "", "text/plain", new byte[0]);
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/ai/resume-extract")
                .file(file)
                .param("userId", "1"))
                .andExpect(status().isInternalServerError()); // empty file triggers RuntimeException
    }

    @Test
    void atsUpload_fallback() throws Exception {
        when(aiService.checkAtsCompatibility(anyLong(), any(), any())).thenThrow(new RuntimeException("Service failure"));
        byte[] txtContent = "Resume content text with skills and experience".getBytes();
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile("file", "test.txt", "text/plain", txtContent);
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/ai/ats-upload")
                .file(file))
                .andExpect(status().isOk());
    }

    @Test
    void analyzeMissingSkills() throws Exception {
        when(aiService.analyzeMissingSkills(any())).thenReturn(new com.resumeai.ai.dto.MissingSkillsResponse());
        com.resumeai.ai.dto.MissingSkillsRequest req = new com.resumeai.ai.dto.MissingSkillsRequest();
        req.setResumeText("some text");
        
        mockMvc.perform(post("/ai/missing-skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void extractResume_unsupportedType() throws Exception {
        byte[] content = "some content".getBytes();
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile("file", "test.jpg", "image/jpeg", content);
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/ai/resume-extract")
                .file(file))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void extractResume_nullFile() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/ai/resume-extract"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void extractResume_json_null() throws Exception {
        mockMvc.perform(post("/ai/resume-extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void extractResume_json_emptyText() throws Exception {
        com.resumeai.ai.dto.ResumeExtractRequest req = new com.resumeai.ai.dto.ResumeExtractRequest();
        req.setResumeText("");
        
        mockMvc.perform(post("/ai/resume-extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }
}
