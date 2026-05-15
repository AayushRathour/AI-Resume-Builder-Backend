package com.resumeai.jobmatch.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.jobmatch.dto.AnalysisResponse;
import com.resumeai.jobmatch.dto.BookmarkRequest;
import com.resumeai.jobmatch.dto.MatchRequest;
import com.resumeai.jobmatch.dto.MatchResponse;
import com.resumeai.jobmatch.service.AdzunaService;
import com.resumeai.jobmatch.service.JobMatchService;
import com.resumeai.jobmatch.service.TheirStackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(JobMatchController.class)
@AutoConfigureMockMvc(addFilters = false)
class JobMatchControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private JobMatchService jobMatchService;
    @MockBean private AdzunaService adzunaService;
    @MockBean private TheirStackService theirStackService;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void analyze() throws Exception {
        AnalysisResponse response = new AnalysisResponse();
        when(jobMatchService.analyzeAndMatchDetailed(any(), anyLong(), anyLong(), anyString(), anyString())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "pdf content".getBytes());
        mockMvc.perform(MockMvcRequestBuilders.multipart("/job-matches/analyze")
                .file(file)
                .param("resumeId", "1")
                .param("userId", "1")
                .param("jobTitle", "Dev")
                .param("location", "US"))
                .andExpect(status().isOk());
    }

    @Test
    void search() throws Exception {
        when(adzunaService.fetchJobs(anyString())).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/job-matches/search").param("query", "java"))
                .andExpect(status().isOk());
    }

    @Test
    void getRankedJobs() throws Exception {
        when(jobMatchService.getRankedJobs(1L)).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/job-matches/jobs").param("userId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void getSavedJobs() throws Exception {
        when(jobMatchService.fetchSavedJobs()).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/job-matches/saved-jobs"))
                .andExpect(status().isOk());
    }

    @Test
    void getTopMatches() throws Exception {
        when(jobMatchService.getTopMatches(1L, 10)).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/job-matches/top").param("userId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void bookmark() throws Exception {
        BookmarkRequest request = new BookmarkRequest();
        request.setMatchId(UUID.randomUUID());
        request.setBookmarked(true);
        when(jobMatchService.updateBookmark(any(), anyBoolean())).thenReturn(new MatchResponse());
        mockMvc.perform(post("/job-matches/bookmark")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void getJobs() throws Exception {
        when(jobMatchService.fetchJobs(any())).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/job-matches").param("keyword", "java"))
                .andExpect(status().isOk());
    }

    @Test
    void matchResumeWithJobs() throws Exception {
        MatchRequest request = new MatchRequest();
        request.setUserId(1L);
        request.setResumeId(1L);
        when(jobMatchService.matchResumeWithJobs(any(), any(), any(), any())).thenReturn(new ArrayList<>());
        mockMvc.perform(post("/job-matches/match")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void getMatchesForUser() throws Exception {
        when(jobMatchService.getMatchesForUser(1L)).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/job-matches/matches/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getMatchesForResume() throws Exception {
        when(jobMatchService.getMatchesForResume(1L)).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/job-matches/matches/resume/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getMatchById() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobMatchService.getMatchById(id)).thenReturn(new MatchResponse());
        mockMvc.perform(get("/job-matches/matches/id/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void toggleBookmark() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobMatchService.toggleBookmark(id)).thenReturn(new MatchResponse());
        mockMvc.perform(put("/job-matches/matches/" + id + "/bookmark"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteMatch() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(jobMatchService).deleteMatch(id);
        mockMvc.perform(delete("/job-matches/matches/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void analyze_missing_resume() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/job-matches/analyze")
                .param("userId", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analyze_missing_userId() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "pdf content".getBytes());
        mockMvc.perform(MockMvcRequestBuilders.multipart("/job-matches/analyze")
                .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory() throws Exception {
        when(jobMatchService.getMatchesForUser(anyLong())).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/job-matches/matches/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
