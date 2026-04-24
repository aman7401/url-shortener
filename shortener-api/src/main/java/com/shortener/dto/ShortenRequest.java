package com.shortener.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public class ShortenRequest {

    @NotBlank(message = "URL must not be blank")
    @URL(message = "Must be a valid URL")
    private String url;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
