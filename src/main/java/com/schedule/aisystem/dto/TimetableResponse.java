package com.schedule.aisystem.dto;

import java.util.List;
import java.util.Map;

public record TimetableResponse(
        String ownerType,
        String ownerName,
        int week,
        int totalWeeks,
        Map<String, List<ScheduleEntryView>> entriesByDay
) {
}

