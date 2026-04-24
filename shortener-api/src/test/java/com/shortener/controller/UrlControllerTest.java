package com.shortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortener.dto.ShortenRequest;
import com.shortener.dto.ShortenResponse;
import com.shortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private UrlService urlService;

    @Test
    void health_returns200WithStatusOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void shorten_returns201WithShortCode() throws Exception {
        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://google.com");

        when(urlService.shorten(any())).thenReturn(new ShortenResponse("abc123", "/r/abc123"));

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.shortUrl").value("/r/abc123"));
    }

    @Test
    void shorten_returns400_whenUrlIsBlank() throws Exception {
        ShortenRequest request = new ShortenRequest();
        request.setUrl("");

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shorten_returns400_whenUrlIsInvalid() throws Exception {
        ShortenRequest request = new ShortenRequest();
        request.setUrl("not-a-url");

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redirect_redirectsToOriginalUrl() throws Exception {
        when(urlService.resolve("abc123")).thenReturn("https://google.com");

        mockMvc.perform(get("/r/abc123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://google.com"));
    }

    @Test
    void redirect_returns404_whenCodeNotFound() throws Exception {
        when(urlService.resolve("missing"))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "URL not found"));

        mockMvc.perform(get("/r/missing"))
                .andExpect(status().isNotFound());
    }
}
