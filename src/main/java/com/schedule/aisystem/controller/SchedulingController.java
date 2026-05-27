package com.schedule.aisystem.controller;

import com.schedule.aisystem.dto.CurrentUserResponse;
import com.schedule.aisystem.dto.AdminFilterOptions;
import com.schedule.aisystem.dto.GenerationResult;
import com.schedule.aisystem.dto.LoginRequest;
import com.schedule.aisystem.dto.LoginResponse;
import com.schedule.aisystem.dto.SystemSnapshot;
import com.schedule.aisystem.dto.TimetableResponse;
import com.schedule.aisystem.service.AuthService;
import com.schedule.aisystem.service.UniversitySchedulingService;
import com.schedule.aisystem.service.UniversitySeedService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SchedulingController {

    private final AuthService authService;
    private final UniversitySeedService sampleDataService;
    private final UniversitySchedulingService schedulingService;

    public SchedulingController(
            AuthService authService,
            UniversitySeedService sampleDataService,
            UniversitySchedulingService schedulingService) {
        this.authService = authService;
        this.sampleDataService = sampleDataService;
        this.schedulingService = schedulingService;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/auth/me")
    public CurrentUserResponse currentUser(@RequestHeader("X-Account-No") String accountNo) {
        return authService.currentUser(accountNo);
    }

    @PostMapping("/demo/reset")
    public void resetDemoData(@RequestHeader("X-Account-No") String accountNo) {
        authService.requireAdmin(accountNo);
        sampleDataService.resetWithSampleData();
    }

    @PostMapping("/schedules/generate")
    public GenerationResult generateSchedule(@RequestHeader("X-Account-No") String accountNo) {
        authService.requireAdmin(accountNo);
        return schedulingService.generateSchedule();
    }

    @GetMapping("/snapshot")
    public SystemSnapshot snapshot(@RequestHeader("X-Account-No") String accountNo) {
        authService.requireUser(accountNo);
        return schedulingService.snapshot();
    }

    @GetMapping("/timetables/me")
    public TimetableResponse myTimetable(
            @RequestHeader("X-Account-No") String accountNo,
            @RequestParam(defaultValue = "1") int week) {
        return schedulingService.timetableForCurrentUser(authService.requireUser(accountNo), week);
    }

    @GetMapping("/timetables/teachers/{teacherId}")
    public TimetableResponse teacherTimetable(
            @RequestHeader("X-Account-No") String accountNo,
            @PathVariable Long teacherId,
            @RequestParam(defaultValue = "1") int week) {
        authService.requireAdmin(accountNo);
        return schedulingService.teacherTimetable(teacherId, week);
    }

    @GetMapping("/timetables/students/{studentId}")
    public TimetableResponse studentTimetable(
            @RequestHeader("X-Account-No") String accountNo,
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") int week) {
        authService.requireAdmin(accountNo);
        return schedulingService.studentTimetable(studentId, week);
    }

    @GetMapping("/timetables/classes/{classId}")
    public TimetableResponse classTimetable(
            @RequestHeader("X-Account-No") String accountNo,
            @PathVariable Long classId,
            @RequestParam(defaultValue = "1") int week) {
        authService.requireAdmin(accountNo);
        return schedulingService.classTimetable(classId, week);
    }

    @GetMapping("/admin/filter-options")
    public AdminFilterOptions filterOptions(
            @RequestHeader("X-Account-No") String accountNo,
            String major,
            Long classId) {
        authService.requireAdmin(accountNo);
        return schedulingService.filterOptions(major, classId);
    }

    @GetMapping("/config")
    public java.util.Map<String, Object> config() {
        int totalWeeks = schedulingService.getTotalWeeks();
        return java.util.Map.of("totalWeeks", totalWeeks);
    }
}
