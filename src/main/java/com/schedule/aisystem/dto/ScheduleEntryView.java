package com.schedule.aisystem.dto;

public record ScheduleEntryView(
        Long id,
        String courseName,
        String teacherName,
        String teacherCode,
        String studentName,
        String studentNo,
        String major,
        String className,
        String classCode,
        String roomName,
        String dayOfWeek,
        Integer periodIndex,
        Integer weekNumber
) {
}
