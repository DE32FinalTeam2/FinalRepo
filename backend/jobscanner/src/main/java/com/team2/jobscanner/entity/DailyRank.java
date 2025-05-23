package com.team2.jobscanner.entity;
import com.team2.jobscanner.time.AuditTime;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.LocalDateTime;
@Getter
@Entity
public class DailyRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="job_title", length = 4)
    private String jobTitle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="tech_name", referencedColumnName = "tech_name")
    private TechStack techStack;

    @Column(name="count")
    private int count;

    @Column(name="category", length = 20)
    private String category;

    @Embedded
    private AuditTime auditTime;

    public DailyRank() {
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

    //getter setter

    @Override
    public String toString() {
        return "DailyRank{" +
                "id=" + id +
                ", jobTitle='" + jobTitle + '\'' +
                ", category='" + category + '\'' +
                ", count=" + count +
                ", techStack=" + techStack + // TechStack 객체를 포함하려면 toString() 호출
                '}';
    }

}

