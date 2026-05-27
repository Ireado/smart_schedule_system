package com.schedule.aisystem.repository;

import com.schedule.aisystem.model.Student;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Override
    @EntityGraph(attributePaths = {"schoolClass"})
    Optional<Student> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"schoolClass"})
    List<Student> findAll();

    @EntityGraph(attributePaths = {"schoolClass"})
    Optional<Student> findByStudentNo(String studentNo);
}

