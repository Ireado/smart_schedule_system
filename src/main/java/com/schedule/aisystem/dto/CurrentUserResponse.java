package com.schedule.aisystem.dto;

import java.util.List;

public record CurrentUserResponse(
        String accountNo,
        String displayName,
        String role,
        Long teacherId,
        Long studentId,
        Long classId,
        String className,
        String major,
        List<ViewerOption> teacherOptions,
        List<ViewerOption> classOptions,
        List<ViewerOption> studentOptions,
        List<String> majorOptions
) {
}
