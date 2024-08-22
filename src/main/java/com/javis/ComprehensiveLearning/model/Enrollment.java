package com.javis.ComprehensiveLearning.model;

import com.javis.ComprehensiveLearning.constants.EnrollmentStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "EnrollmentDetails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Enrollment_Id")
    private Long enrollmentId;

    @Column(name = "User_Id", nullable = false)
    private Long userId;

    @Column(name = "Course_Id", nullable = false)
    private Long courseId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "Status", nullable = false)
    private EnrollmentStatusEnum status;
}
