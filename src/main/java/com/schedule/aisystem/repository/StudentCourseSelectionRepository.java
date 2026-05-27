package com.schedule.aisystem.repository;

import com.schedule.aisystem.model.StudentCourseSelection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentCourseSelectionRepository extends JpaRepository<StudentCourseSelection, Long> {

    @Override
    @EntityGraph(attributePaths = {"student", "student.schoolClass", "course", "course.teacher", "course.schoolClass"})
    List<StudentCourseSelection> findAll();

    @EntityGraph(attributePaths = {"student", "student.schoolClass", "course", "course.teacher", "course.schoolClass"})
    List<StudentCourseSelection> findByStudent_Id(Long studentId);
}
