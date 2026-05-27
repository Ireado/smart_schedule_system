package com.schedule.aisystem.service;

import com.schedule.aisystem.dto.AdminFilterOptions;
import com.schedule.aisystem.dto.GenerationResult;
import com.schedule.aisystem.dto.ScheduleEntryView;
import com.schedule.aisystem.dto.SystemSnapshot;
import com.schedule.aisystem.dto.TimetableResponse;
import com.schedule.aisystem.dto.ViewerOption;
import com.schedule.aisystem.model.Course;
import com.schedule.aisystem.model.Room;
import com.schedule.aisystem.model.ScheduleEntry;
import com.schedule.aisystem.model.Student;
import com.schedule.aisystem.model.StudentCourseSelection;
import com.schedule.aisystem.model.UserAccount;
import com.schedule.aisystem.model.UserRole;
import com.schedule.aisystem.repository.CourseRepository;
import com.schedule.aisystem.repository.RoomRepository;
import com.schedule.aisystem.repository.ScheduleEntryRepository;
import com.schedule.aisystem.repository.SchoolClassRepository;
import com.schedule.aisystem.repository.StudentCourseSelectionRepository;
import com.schedule.aisystem.repository.StudentRepository;
import com.schedule.aisystem.repository.TeacherRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UniversitySchedulingService {

    private static final List<String> DAYS = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    private static final int PERIODS_PER_DAY = 8;

    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final StudentCourseSelectionRepository selectionRepository;
    private final ScheduleMapper scheduleMapper;

    public UniversitySchedulingService(
            CourseRepository courseRepository,
            RoomRepository roomRepository,
            ScheduleEntryRepository scheduleEntryRepository,
            TeacherRepository teacherRepository,
            SchoolClassRepository schoolClassRepository,
            StudentRepository studentRepository,
            StudentCourseSelectionRepository selectionRepository,
            ScheduleMapper scheduleMapper) {
        this.courseRepository = courseRepository;
        this.roomRepository = roomRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.teacherRepository = teacherRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
        this.selectionRepository = selectionRepository;
        this.scheduleMapper = scheduleMapper;
    }

    @Transactional
    public GenerationResult generateSchedule() {
        List<Course> courses = courseRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<StudentCourseSelection> selections = selectionRepository.findAll();
        if (courses.isEmpty() || rooms.isEmpty() || selections.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先准备课程、教室和学生选课数据");
        }

        scheduleEntryRepository.deleteAllInBatch();
        List<ScheduleEntry> generated = generateLocally(courses, rooms, selections);
        List<ScheduleEntry> saved = scheduleEntryRepository.saveAll(generated);
        return new GenerationResult(true, "排课完成", saved.size(), saved.stream().map(scheduleMapper::toView).toList());
    }

    public SystemSnapshot snapshot() {
        return new SystemSnapshot(
                teacherRepository.findAll().stream()
                        .map(item -> new ViewerOption(item.getId(), item.getTeacherCode(), item.getName())).toList(),
                schoolClassRepository.findAll().stream()
                        .map(item -> new ViewerOption(item.getId(), item.getClassCode(), item.getName())).toList(),
                roomRepository.findAll().stream()
                        .map(item -> new ViewerOption(item.getId(), item.getRoomCode(), item.getName())).toList(),
                studentRepository.findAll().stream()
                        .map(item -> new ViewerOption(item.getId(), item.getStudentNo(), item.getName())).toList(),
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
            return teacherTimetable(account.getTeacher().getId(), week);
        }
        if (account.getRole() == UserRole.STUDENT) {
            return studentTimetable(account.getStudent().getId(), week);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员请使用筛选查询接口");
    }

    public TimetableResponse teacherTimetable(Long teacherId, int week) {
        var teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教师不存在"));
        List<ScheduleEntry> entries = deduplicate(
                scheduleEntryRepository.findByTeacher_IdAndWeekNumber(teacherId, week));
        return new TimetableResponse("teacher", teacher.getName(), week, getTotalWeeks(), groupByDay(entries));
    }

    @Transactional
    public TimetableResponse classTimetable(Long classId, int week) {
        var schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "班级不存在"));
        List<ScheduleEntry> entries = deduplicate(
                scheduleEntryRepository.findBySchoolClass_IdAndWeekNumber(classId, week));
        return new TimetableResponse("class", schoolClass.getName(), week, getTotalWeeks(), groupByDay(entries));
    }

    @Transactional
    public TimetableResponse studentTimetable(Long studentId, int week) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在"));
        List<ScheduleEntry> entries = scheduleEntryRepository.findByStudent_IdAndWeekNumber(studentId, week);
        return new TimetableResponse("student", student.getName() + " - " + student.getSchoolClass().getName(),
                week, getTotalWeeks(), groupByDay(entries));
    }

    public AdminFilterOptions filterOptions(String major, Long classId) {
        var classes = schoolClassRepository.findAll().stream()
                .filter(item -> major == null || major.isBlank() || item.getMajor().equals(major))
                .map(item -> new ViewerOption(item.getId(), item.getClassCode(), item.getName()))
                .toList();

        Set<Long> allowedClassIds = classes.stream().map(ViewerOption::id).collect(Collectors.toSet());
        final Set<Long> finalAllowedClassIds = classId != null ? Set.of(classId) : allowedClassIds;

        var students = studentRepository.findAll().stream()
                .filter(item -> finalAllowedClassIds.contains(item.getSchoolClass().getId()))
                .map(item -> new ViewerOption(item.getId(), item.getStudentNo(), item.getName()))
                .toList();

        var teachers = courseRepository.findAll().stream()
                .filter(item -> finalAllowedClassIds.contains(item.getSchoolClass().getId()))
                .map(Course::getTeacher)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(item -> item.getId(), item -> item, (a, b) -> a, LinkedHashMap::new),
                        map -> map.values().stream()
                                .map(item -> new ViewerOption(item.getId(), item.getTeacherCode(), item.getName()))
                                .toList()));

        return new AdminFilterOptions(teachers, students, classes);
    }

    public List<Integer> availableWeeks() {
        return scheduleEntryRepository.findDistinctWeekNumbers();
    }

    private Map<String, List<ScheduleEntryView>> groupByDay(List<ScheduleEntry> entries) {
        Map<String, List<ScheduleEntryView>> result = new LinkedHashMap<>();
        for (String day : DAYS) {
            result.put(day, entries.stream()
                    .filter(item -> day.equals(item.getDayOfWeek()))
                    .sorted(Comparator.comparing(ScheduleEntry::getPeriodIndex))
                    .map(scheduleMapper::toView)
                    .toList());
        }
        return result;
    }

    private List<ScheduleEntry> deduplicate(List<ScheduleEntry> entries) {
        Map<String, ScheduleEntry> unique = new LinkedHashMap<>();
        for (ScheduleEntry entry : entries) {
            String key = entry.getCourse().getId() + "|" + entry.getSchoolClass().getId() + "|"
                    + entry.getDayOfWeek() + "|" + entry.getPeriodIndex() + "|" + entry.getRoom().getId()
                    + "|" + entry.getWeekNumber();
            unique.putIfAbsent(key, entry);
        }
        return new ArrayList<>(unique.values());
    }

    private List<ScheduleEntry> generateLocally(List<Course> courses, List<Room> rooms,
            List<StudentCourseSelection> selections) {
        int totalWeeks = courses.stream()
                .mapToInt(Course::getTotalWeeks)
                .max()
                .orElse(18);

        List<SlotAssignment> allSlotAssignments = new ArrayList<>();

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
                    .comparingInt((CourseUnit item) -> item.course().getSchoolClass().getStudentCount()).reversed()
                    .thenComparingInt(item -> item.course().getTotalHours()));

            Map<String, Set<Long>> teacherBusy = new LinkedHashMap<>();
            Map<String, Set<Long>> classBusy = new LinkedHashMap<>();
            Map<String, Set<Long>> roomBusy = new LinkedHashMap<>();
            List<SlotAssignment> weekAssignments = new ArrayList<>();

            for (CourseUnit unit : units) {
                Placement placement = findPlacement(unit.course(), rooms, teacherBusy, classBusy, roomBusy,
                        weekAssignments);
                if (placement == null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "第%d周: 无法为课程 %s 分配无冲突时间片".formatted(currentWeek, unit.course().getName()));
                }
                weekAssignments.add(new SlotAssignment(unit.course(), placement.day(), placement.period(),
                        placement.room(), currentWeek));
                allSlotAssignments.add(new SlotAssignment(unit.course(), placement.day(), placement.period(),
                        placement.room(), currentWeek));
                occupy(teacherBusy, placement.day(), placement.period(), unit.course().getTeacher().getId());
                occupy(classBusy, placement.day(), placement.period(), unit.course().getSchoolClass().getId());
                occupy(roomBusy, placement.day(), placement.period(), placement.room().getId());
            }
        }

        Map<Long, List<SlotAssignment>> slotsByCourse = allSlotAssignments.stream()
                .collect(Collectors.groupingBy(item -> item.course().getId()));
        List<ScheduleEntry> entries = new ArrayList<>();
        for (StudentCourseSelection selection : selections) {
            List<SlotAssignment> slots = slotsByCourse.getOrDefault(selection.getCourse().getId(), List.of());
            for (SlotAssignment slot : slots) {
                ScheduleEntry entry = new ScheduleEntry();
                entry.setCourse(selection.getCourse());
                entry.setTeacher(selection.getCourse().getTeacher());
                entry.setSchoolClass(selection.getStudent().getSchoolClass());
                entry.setStudent(selection.getStudent());
                entry.setRoom(slot.room());
                entry.setDayOfWeek(slot.day());
                entry.setPeriodIndex(slot.period());
                entry.setWeekNumber(slot.weekNumber());
                entries.add(entry);
            }
        }
        return entries;
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
            List<SlotAssignment> assignments) {
        Map<String, Long> classLoadPerDay = new LinkedHashMap<>();
        Map<String, Long> teacherLoadPerDay = new LinkedHashMap<>();
        for (String day : DAYS) {
            long classLoad = assignments.stream()
                    .filter(item -> item.course().getSchoolClass().getId().equals(course.getSchoolClass().getId()))
                    .filter(item -> day.equals(item.day()))
                    .count();
            classLoadPerDay.put(day, classLoad);
            long teacherLoad = assignments.stream()
                    .filter(item -> item.course().getTeacher().getId().equals(course.getTeacher().getId()))
                    .filter(item -> day.equals(item.day()))
                    .count();
            teacherLoadPerDay.put(day, teacherLoad);
        }

        List<String> sortedDays = new ArrayList<>(DAYS);
        sortedDays.sort(Comparator
                .comparingLong((String day) -> classLoadPerDay.get(day))
                .thenComparingLong(teacherLoadPerDay::get));

        for (String day : sortedDays) {
            long classLoadForDay = classLoadPerDay.get(day);
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

    private record SlotAssignment(Course course, String day, int period, Room room, int weekNumber) {
    }
}
