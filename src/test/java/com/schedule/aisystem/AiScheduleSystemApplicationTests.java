package com.schedule.aisystem;

import static org.assertj.core.api.Assertions.assertThat;

import com.schedule.aisystem.dto.LoginRequest;
import com.schedule.aisystem.dto.GenerationResult;
import com.schedule.aisystem.service.AuthService;
import com.schedule.aisystem.service.UniversitySchedulingService;
import com.schedule.aisystem.service.UniversitySeedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiScheduleSystemApplicationTests {

    @Autowired
    private UniversitySeedService sampleDataService;

    @Autowired
    private UniversitySchedulingService schedulingService;

    @Autowired
    private AuthService authService;

    @Test
    void shouldGenerateScheduleWithoutConflictsForSampleData() {
        sampleDataService.resetWithSampleData();

        GenerationResult result = schedulingService.generateSchedule();

        assertThat(result.success()).isTrue();
        assertThat(result.generatedCount()).isGreaterThan(0);
        assertThat(schedulingService.snapshot().schedules()).hasSize(result.generatedCount());
    }

    @Test
    void studentAccountShouldLoginAndLoadPersonalTimetable() {
        sampleDataService.resetWithSampleData();
        schedulingService.generateSchedule();

        var login = authService.login(new LoginRequest("100000", "000000"));
        var me = authService.currentUser("100000");
        var timetable = schedulingService.studentTimetable(me.studentId(), 1);

        assertThat(login.role()).isEqualTo("STUDENT");
        assertThat(me.studentId()).isNotNull();
        assertThat(timetable.ownerType()).isEqualTo("student");
        assertThat(timetable.entriesByDay()).isNotEmpty();
    }
}
