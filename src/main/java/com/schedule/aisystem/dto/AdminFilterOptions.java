package com.schedule.aisystem.dto;

import java.util.List;

public record AdminFilterOptions(
        List<ViewerOption> teachers,
        List<ViewerOption> students,
        List<ViewerOption> classes
) {
}
