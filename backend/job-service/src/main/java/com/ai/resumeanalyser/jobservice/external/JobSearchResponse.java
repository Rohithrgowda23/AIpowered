package com.ai.resumeanalyser.jobservice.external;

import lombok.Data;

import java.util.List;

@Data
public class JobSearchResponse {

    private List<Job> results;
    private int count;
    private double mean;
}
