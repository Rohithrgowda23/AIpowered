package com.ai.resumeanalyser.reportservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@jakarta.persistence.Table(name = "previous_table")
public class PreviousTable {

    @Id
    private String email;

    private int score;
    private int atsoptimizationscore;
    private String roles;

    @Column(length = 1000)
    private String summary;

    private String experienceLevel;

    @ElementCollection
    @CollectionTable(name = "previous_table_skills", joinColumns = @JoinColumn(name = "previous_table_email"))
    @Column(name = "skills", length = 450)
    private List<String> skills;

    @ElementCollection
    @CollectionTable(name = "previous_table_missing_skills", joinColumns = @JoinColumn(name = "previous_table_email"))
    @Column(name = "missing_skills", length = 450)
    private List<String> missingSkills;

    @ElementCollection
    @CollectionTable(name = "previous_table_strengths", joinColumns = @JoinColumn(name = "previous_table_email"))
    @Column(name = "strengths", length = 450)
    private List<String> strengths;

    @ElementCollection
    @CollectionTable(name = "previous_table_weaknesses", joinColumns = @JoinColumn(name = "previous_table_email"))
    @Column(name = "weaknesses", length = 450)
    private List<String> weaknesses;

    @ElementCollection
    @CollectionTable(name = "previous_table_interview_tips", joinColumns = @JoinColumn(name = "previous_table_email"))
    @Column(name = "interview_tips", length = 450)
    private List<String> interviewTips;

    @ElementCollection
    @CollectionTable(name = "previous_table_pros", joinColumns = @JoinColumn(name = "previous_table_email"))
    @Column(name = "pros", length = 450)
    private List<String> pros;

    @ElementCollection
    @CollectionTable(name = "previous_table_cons", joinColumns = @JoinColumn(name = "previous_table_email"))
    @Column(name = "cons", length = 450)
    private List<String> cons;

    @ElementCollection
    @CollectionTable(name = "previous_table_suggestions", joinColumns = @JoinColumn(name = "previous_table_email"))
    @Column(name = "suggestions", length = 450)
    private List<String> suggestions;


    @Column(length = 6000)
    private String jobDescription;


    private String jdExperienceLevel;

    @ElementCollection
    @CollectionTable(name = "previous_table_jd_skills", joinColumns = @JoinColumn(name = "previous_table_email"))
    @Column(name = "jd_skill", length = 450)
    private List<String> jdSkills;

    @ElementCollection
    @CollectionTable(name = "previous_table_jd_technologies", joinColumns = @JoinColumn(name = "previous_table_email"))
    @Column(name = "jd_technology", length = 450)
    private List<String> jdTechnologies;

    @ElementCollection
    @CollectionTable(name = "previous_table_jd_keywords", joinColumns = @JoinColumn(name = "previous_table_email"))
    @Column(name = "jd_keyword", length = 450)
    private List<String> jdKeywords;

    @ElementCollection
    @CollectionTable(name = "previous_table_jd_responsibilities", joinColumns = @JoinColumn(name = "previous_table_email"))
    @Column(name = "jd_responsibility", length = 450)
    private List<String> jdResponsibilities;

    @ElementCollection
    @CollectionTable(name = "previous_table_jd_qualifications", joinColumns = @JoinColumn(name = "previous_table_email"))
    @Column(name = "jd_qualification", length = 450)
    private List<String> jdQualifications;

    private String jdLocation;
}
