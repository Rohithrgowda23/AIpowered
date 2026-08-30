package com.ai.resumeanalyser.jobservice.dto;

public record JobDto(
        String id,
        String title,
        String description,
        String redirect_url,
        CompanyDto company,
        LocationDto location,
        CategoryDto category
) {
    public record CompanyDto(String display_name) {}
    public record LocationDto(String display_name) {}
    public record CategoryDto(String tag, String label) {}
}
