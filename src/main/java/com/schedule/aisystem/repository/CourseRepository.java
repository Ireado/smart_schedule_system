package com.schedule.aisystem.repository;

import com.schedule.aisystem.model.Course;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Override
    @EntityGraph(attributePaths = {"teacher", "schoolClass"})
    List<Course> findAll();
}

