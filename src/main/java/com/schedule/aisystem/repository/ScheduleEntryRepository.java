package com.schedule.aisystem.repository;

import com.schedule.aisystem.model.ScheduleEntry;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Long> {

    @Override
    @EntityGraph(attributePaths = {"course", "teacher", "schoolClass", "student", "room"})
    List<ScheduleEntry> findAll();

    @EntityGraph(attributePaths = {"course", "teacher", "schoolClass", "student", "room"})
    List<ScheduleEntry> findByTeacher_Id(Long teacherId);

    @EntityGraph(attributePaths = {"course", "teacher", "schoolClass", "student", "room"})
    List<ScheduleEntry> findBySchoolClass_Id(Long classId);

    @EntityGraph(attributePaths = {"course", "teacher", "schoolClass", "student", "room"})
    List<ScheduleEntry> findByStudent_Id(Long studentId);

    @EntityGraph(attributePaths = {"course", "teacher", "schoolClass", "student", "room"})
    List<ScheduleEntry> findByTeacher_IdAndWeekNumber(Long teacherId, Integer weekNumber);

    @EntityGraph(attributePaths = {"course", "teacher", "schoolClass", "student", "room"})
    List<ScheduleEntry> findBySchoolClass_IdAndWeekNumber(Long classId, Integer weekNumber);

    @EntityGraph(attributePaths = {"course", "teacher", "schoolClass", "student", "room"})
    List<ScheduleEntry> findByStudent_IdAndWeekNumber(Long studentId, Integer weekNumber);

    @Query("SELECT DISTINCT s.weekNumber FROM ScheduleEntry s ORDER BY s.weekNumber")
    List<Integer> findDistinctWeekNumbers();
}
