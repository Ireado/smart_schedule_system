package com.schedule.aisystem.repository;

import com.schedule.aisystem.model.Teacher;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByTeacherCode(String teacherCode);
}

