package com.team2.jobscanner.entity;
import com.team2.jobscanner.time.AuditTime;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
public class JobRole {

    @Id
    @Column(name="job_title", length = 4, nullable = false)
    private String jobTitle;

    @OneToMany(mappedBy = "jobRoles")
    private List<Notice> notices;

    @Column(name="role_name", length = 100, nullable = false)
    private String roleName;

    @Column(name="role_description", columnDefinition = "TEXT", nullable = false)
    private String roleDescription;


    @Embedded
    private AuditTime auditTime;

    public JobRole() {
        this.auditTime = new AuditTime();
    }

    @PrePersist
    public void onPrePersist() {
        // 새 데이터가 삽입될 때만 create_time은 현재 시간으로 설정됨
        if (this.auditTime.getCreateTime() == null) {
            this.auditTime.setCreateTime(LocalDateTime.now());
        }
        this.auditTime.setUpdateTime(LocalDateTime.now());  // update_time은 삽입 시점에 설정됨
    }

    @PreUpdate
    public void onPreUpdate() {
        this.auditTime.setUpdateTime(LocalDateTime.now());  // 데이터가 수정될 때마다 update_time 갱신
    }

  

    // Getter, Setter
}
