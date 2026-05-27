package com.schedule.aisystem.service;

import com.schedule.aisystem.dto.ScheduleEntryView;
import com.schedule.aisystem.model.ScheduleEntry;
import org.springframework.stereotype.Component;

@Component
public class ScheduleMapper {

    public ScheduleEntryView toView(ScheduleEntry entry) {
        return new ScheduleEntryView(
                entry.getId(),
                entry.getCourse().getName(),
                entry.getTeacher().getName(),
                entry.getTeacher().getTeacherCode(),
                entry.getStudent() == null ? null : entry.getStudent().getName(),
                entry.getStudent() == null ? null : entry.getStudent().getStudentNo(),
                entry.getSchoolClass().getMajor(),
                entry.getSchoolClass().getName(),
                entry.getSchoolClass().getClassCode(),
                entry.getRoom().getName(),
                entry.getDayOfWeek(),
                entry.getPeriodIndex(),
                entry.getWeekNumber()
        );
    }
}
