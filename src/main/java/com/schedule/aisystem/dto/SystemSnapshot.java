package com.schedule.aisystem.dto;

import java.util.List;

public record SystemSnapshot(
        List<ViewerOption> teachers,
        List<ViewerOption> classes,
        List<ViewerOption> rooms,
        List<ViewerOption> students,
        List<ScheduleEntryView> schedules
) {
}
