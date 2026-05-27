package com.schedule.aisystem.service;

import com.schedule.aisystem.dto.GenerationResult;
import com.schedule.aisystem.dto.ScheduleEntryView;
import com.schedule.aisystem.dto.SystemSnapshot;
import com.schedule.aisystem.dto.TimetableResponse;
import com.schedule.aisystem.dto.ViewerOption;
import com.schedule.aisystem.model.Course;
import com.schedule.aisystem.model.Room;
import com.schedule.aisystem.model.ScheduleEntry;
import com.schedule.aisystem.model.Student;
import com.schedule.aisystem.model.UserAccount;
import com.schedule.aisystem.model.UserRole;
import com.schedule.aisystem.repository.CourseRepository;
import com.schedule.aisystem.repository.RoomRepository;
import com.schedule.aisystem.repository.ScheduleEntryRepository;
import com.schedule.aisystem.repository.SchoolClassRepository;
import com.schedule.aisystem.repository.StudentRepository;
import com.schedule.aisystem.repository.TeacherRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SchedulingService {

    private static final List<String> DAYS = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    private static final int PERIODS_PER_DAY = 6;

    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final AiSchedulingClient aiSchedulingClient;
    private final ScheduleMapper scheduleMapper;

    public SchedulingService(
            CourseRepository courseRepository,
            RoomRepository roomRepository,
            ScheduleEntryRepository scheduleEntryRepository,
            TeacherRepository teacherRepository,
            SchoolClassRepository schoolClassRepository,
            StudentRepository studentRepository,
            AiSchedulingClient aiSchedulingClient,
            ScheduleMapper scheduleMapper) {
        this.courseRepository = courseRepository;
        this.roomRepository = roomRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.teacherRepository = teacherRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
        this.aiSchedulingClient = aiSchedulingClient;
        this.scheduleMapper = scheduleMapper;
    }

    @Transactional
    public GenerationResult generateSchedule() {
        List<Course> courses = courseRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        if (courses.isEmpty() || rooms.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先准备课程与教室数据");
        }

        scheduleEntryRepository.deleteAllInBatch();

        List<ScheduleEntry> generated = aiSchedulingClient.generate(courses, rooms)
                .orElseGet(() -> generateLocally(courses, rooms));
        List<ScheduleEntry> saved = scheduleEntryRepository.saveAll(generated);
        return new GenerationResult(
                true,
                "排课完成",
                saved.size(),
                saved.stream().map(scheduleMapper::toView).toList());
    }

    public SystemSnapshot snapshot() {
        return new SystemSnapshot(
                teacherRepository.findAll().stream()
                        .map(item -> new ViewerOption(item.getId(), item.getTeacherCode(), item.getName()))
                        .toList(),
                schoolClassRepository.findAll().stream()
                        .map(item -> new ViewerOption(item.getId(), item.getClassCode(), item.getName()))
                        .toList(),
                roomRepository.findAll().stream()
                        .map(item -> new ViewerOption(item.getId(), item.getRoomCode(), item.getName()))
                        .toList(),
                studentRepository.findAll().stream()
                        .map(item -> new ViewerOption(item.getId(), item.getStudentNo(), item.getName()))
                        .toList(),
                scheduleEntryRepository.findAll().stream().map(scheduleMapper::toView).toList());
    }

    public int getTotalWeeks() {
        List<Course> courses = courseRepository.findAll();
        return courses.stream()
                .mapToInt(Course::getTotalWeeks)
                .max()
                .orElse(18);
    }

    public TimetableResponse timetableForCurrentUser(UserAccount account, int week) {
        if (account.getRole() == UserRole.TEACHER) {
            if (account.getTeacher() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教师账号未绑定教师信息");
            }
            return teacherTimetable(account.getTeacher().getId(), week);
        }
        if (account.getRole() == UserRole.STUDENT) {
            if (account.getStudent() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学生账号未绑定学生信息");
            }
            return studentTimetable(account.getStudent().getId(), week);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员请使用指定查询接口");
    }

    public TimetableResponse teacherTimetable(Long teacherId, int week) {
        var teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教师不存在"));
        return new TimetableResponse(
                "teacher",
                teacher.getName(),
                week,
                getTotalWeeks(),
                groupByDay(scheduleEntryRepository.findByTeacher_IdAndWeekNumber(teacherId, week)));
    }

    public TimetableResponse classTimetable(Long classId, int week) {
        var schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "班级不存在"));
        return new TimetableResponse(
                "class",
                schoolClass.getName(),
                week,
                getTotalWeeks(),
                groupByDay(scheduleEntryRepository.findBySchoolClass_IdAndWeekNumber(classId, week)));
    }

    public TimetableResponse studentTimetable(Long studentId, int week) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在"));
        return new TimetableResponse(
                "student",
                student.getName() + " - " + student.getSchoolClass().getName(),
                week,
                getTotalWeeks(),
                groupByDay(scheduleEntryRepository.findBySchoolClass_IdAndWeekNumber(
                        student.getSchoolClass().getId(), week)));
    }

    public List<Integer> availableWeeks() {
        return scheduleEntryRepository.findDistinctWeekNumbers();
    }

    private Map<String, List<ScheduleEntryView>> groupByDay(List<ScheduleEntry> entries) {
        Map<String, List<ScheduleEntryView>> map = new LinkedHashMap<>();
        for (String day : DAYS) {
            List<ScheduleEntryView> items = entries.stream()
                    .filter(entry -> day.equals(entry.getDayOfWeek()))
                    .sorted(Comparator.comparing(ScheduleEntry::getPeriodIndex))
                    .map(scheduleMapper::toView)
                    .toList();
            map.put(day, items);
        }
        return map;
    }

    private List<ScheduleEntry> generateLocally(List<Course> courses, List<Room> rooms) {
        int totalWeeks = courses.stream()
                .mapToInt(Course::getTotalWeeks)
                .max()
                .orElse(18);

        List<ScheduleEntry> result = new ArrayList<>();

        for (int week = 1; week <= totalWeeks; week++) {
            final int currentWeek = week;
            List<CourseUnit> units = new ArrayList<>();
            for (Course course : courses) {
                int periodsThisWeek = periodsForWeek(course, currentWeek, totalWeeks);
                for (int i = 0; i < periodsThisWeek; i++) {
                    units.add(new CourseUnit(course, i + 1));
                }
            }

            units.sort(Comparator
                    .comparingInt((CourseUnit unit) -> unit.course().getSchoolClass().getStudentCount()).reversed()
                    .thenComparingInt(unit -> unit.course().getTotalHours()));

            Map<String, Set<Long>> teacherBusy = new LinkedHashMap<>();
            Map<String, Set<Long>> classBusy = new LinkedHashMap<>();
            Map<String, Set<Long>> roomBusy = new LinkedHashMap<>();
            List<ScheduleEntry> weekEntries = new ArrayList<>();

            for (CourseUnit unit : units) {
                Placement placement = findPlacement(unit.course(), rooms, teacherBusy, classBusy, roomBusy,
                        weekEntries);
                if (placement == null) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "第%d周: 无法为课程 %s 分配无冲突时间片".formatted(currentWeek, unit.course().getName()));
                }

                ScheduleEntry entry = new ScheduleEntry();
                entry.setCourse(unit.course());
                entry.setTeacher(unit.course().getTeacher());
                entry.setSchoolClass(unit.course().getSchoolClass());
                entry.setRoom(placement.room());
                entry.setDayOfWeek(placement.day());
                entry.setPeriodIndex(placement.period());
                entry.setWeekNumber(currentWeek);
                weekEntries.add(entry);
                result.add(entry);

                occupy(teacherBusy, placement.day(), placement.period(), unit.course().getTeacher().getId());
                occupy(classBusy, placement.day(), placement.period(), unit.course().getSchoolClass().getId());
                occupy(roomBusy, placement.day(), placement.period(), placement.room().getId());
            }
        }

        return result;
    }

    private int periodsForWeek(Course course, int weekNumber, int totalWeeks) {
        int totalHours = course.getTotalHours();
        int basePerWeek = totalHours / totalWeeks;
        int extraWeeks = totalHours % totalWeeks;
        return basePerWeek + (weekNumber <= extraWeeks ? 1 : 0);
    }

    private Placement findPlacement(
            Course course,
            List<Room> rooms,
            Map<String, Set<Long>> teacherBusy,
            Map<String, Set<Long>> classBusy,
            Map<String, Set<Long>> roomBusy,
            List<ScheduleEntry> currentEntries) {
        Map<String, Integer> classLoadPerDay = new LinkedHashMap<>();
        Map<String, Integer> teacherLoadPerDay = new LinkedHashMap<>();
        for (String day : DAYS) {
            int classLoad = (int) currentEntries.stream()
                    .filter(entry -> entry.getSchoolClass().getId().equals(course.getSchoolClass().getId()))
                    .filter(entry -> day.equals(entry.getDayOfWeek()))
                    .count();
            classLoadPerDay.put(day, classLoad);
            int teacherLoad = (int) currentEntries.stream()
                    .filter(entry -> entry.getTeacher().getId().equals(course.getTeacher().getId()))
                    .filter(entry -> day.equals(entry.getDayOfWeek()))
                    .count();
            teacherLoadPerDay.put(day, teacherLoad);
        }

        List<String> sortedDays = new ArrayList<>(DAYS);
        sortedDays.sort(Comparator
                .comparingInt((String day) -> classLoadPerDay.get(day))
                .thenComparingInt(teacherLoadPerDay::get));

        for (String day : sortedDays) {
            int classLoadForDay = classLoadPerDay.get(day);
            if (classLoadForDay >= PERIODS_PER_DAY) {
                continue;
            }

            for (int period = 1; period <= PERIODS_PER_DAY; period++) {
                final int currentPeriod = period;
                if (isBusy(teacherBusy, day, period, course.getTeacher().getId())
                        || isBusy(classBusy, day, period, course.getSchoolClass().getId())) {
                    continue;
                }

                Optional<Room> room = rooms.stream()
                        .filter(item -> item.getCapacity() >= course.getSchoolClass().getStudentCount())
                        .filter(item -> !isBusy(roomBusy, day, currentPeriod, item.getId()))
                        .sorted(Comparator.comparing(Room::getCapacity))
                        .findFirst();
                if (room.isPresent()) {
                    return new Placement(day, period, room.get());
                }
            }
        }
        return null;
    }

    private boolean isBusy(Map<String, Set<Long>> busyMap, String day, int period, Long entityId) {
        return busyMap.getOrDefault(slotKey(day, period), Set.of()).contains(entityId);
    }

    private void occupy(Map<String, Set<Long>> busyMap, String day, int period, Long entityId) {
        busyMap.computeIfAbsent(slotKey(day, period), key -> new LinkedHashSet<>()).add(entityId);
    }

    private String slotKey(String day, int period) {
        return day + "-" + period;
    }

    private record CourseUnit(Course course, int sequence) {
    }

    private record Placement(String day, int period, Room room) {
    }
}
