package com.schedule.aisystem.repository;

import com.schedule.aisystem.model.SchoolClass;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    Optional<SchoolClass> findByClassCode(String classCode);
}

