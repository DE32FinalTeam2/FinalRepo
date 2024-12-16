package com.team2.jobscanner.entity;

import com.team2.jobscanner.time.AuditTime;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;


@Entity
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "id")
    private List<JobRole> jobRoles;

    @Column(name="job_role_id",nullable = false)
    private Long jobRoleId;

    @Column(name = "due_type", length = 20, nullable = false)
    private String dueType;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "company_name", length = 255, nullable = false)
    private String companyName;

    @Column(name = "notice_title", length = 255, nullable = false)
    private String noticeTitle;

    @Column(name="main_task",columnDefinition = "TEXT")
    private String mainTask;

    @Column(name="tech_required",columnDefinition = "TEXT")
    private String techRequired;

    @Column(name="tech_prefer",columnDefinition = "TEXT")
    private String techPrefer;

    @Column(name = "tech_list", length = 1000)

    @Embedded
    private AuditTime auditTime;

    // Getter, Setter
}
