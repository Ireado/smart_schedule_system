package com.schedule.aisystem.dto;

import java.util.List;

public record GenerationResult(
        boolean success,
        String message,
        int generatedCount,
        List<ScheduleEntryView> entries
) {
}

