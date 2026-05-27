package com.schedule.aisystem.service;

import com.schedule.aisystem.model.Course;
import com.schedule.aisystem.model.Room;
import com.schedule.aisystem.model.SchoolClass;
import com.schedule.aisystem.model.Teacher;
import com.schedule.aisystem.repository.CourseRepository;
import com.schedule.aisystem.repository.RoomRepository;
import com.schedule.aisystem.repository.SchoolClassRepository;
import com.schedule.aisystem.repository.ScheduleEntryRepository;
import com.schedule.aisystem.repository.TeacherRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SampleDataService {

    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final RoomRepository roomRepository;
    private final CourseRepository courseRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;

    public SampleDataService(
            TeacherRepository teacherRepository,
            SchoolClassRepository schoolClassRepository,
            RoomRepository roomRepository,
            CourseRepository courseRepository,
            ScheduleEntryRepository scheduleEntryRepository) {
        this.teacherRepository = teacherRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.roomRepository = roomRepository;
        this.courseRepository = courseRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
    }

    @Transactional
    public void resetWithSampleData() {
        scheduleEntryRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        teacherRepository.deleteAllInBatch();
        schoolClassRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();

        Teacher math = teacher("T001", "张敏", "数学");
        Teacher chinese = teacher("T002", "李华", "语文");
        Teacher english = teacher("T003", "王蕾", "英语");
        Teacher physics = teacher("T004", "陈涛", "物理");
        teacherRepository.saveAll(List.of(math, chinese, english, physics));

        SchoolClass classOne = schoolClass("C101", "高一(1)班", 42);
        SchoolClass classTwo = schoolClass("C102", "高一(2)班", 40);
        SchoolClass classThree = schoolClass("C201", "高二(1)班", 38);
        schoolClassRepository.saveAll(List.of(classOne, classTwo, classThree));

        Room roomA = room("R101", "教学楼101", 50);
        Room roomB = room("R102", "教学楼102", 50);
        Room roomC = room("R201", "实验楼201", 45);
        roomRepository.saveAll(List.of(roomA, roomB, roomC));

        courseRepository.saveAll(List.of(
                course("数学", 4, math, classOne),
                course("语文", 4, chinese, classOne),
                course("英语", 3, english, classOne),
                course("物理", 2, physics, classOne),
                course("数学", 4, math, classTwo),
                course("语文", 4, chinese, classTwo),
                course("英语", 3, english, classTwo),
                course("物理", 2, physics, classTwo),
                course("数学", 4, math, classThree),
                course("语文", 4, chinese, classThree),
                course("英语", 3, english, classThree),
                course("物理", 3, physics, classThree)));
    }

    private Teacher teacher(String code, String name, String subject) {
        Teacher teacher = new Teacher();
        teacher.setTeacherCode(code);
        teacher.setName(name);
        teacher.setSubject(subject);
        return teacher;
    }

    private SchoolClass schoolClass(String code, String name, int studentCount) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassCode(code);
        schoolClass.setName(name);
        schoolClass.setStudentCount(studentCount);
        return schoolClass;
    }

    private Room room(String code, String name, int capacity) {
        Room room = new Room();
        room.setRoomCode(code);
        room.setName(name);
        room.setCapacity(capacity);
        return room;
    }

    private Course course(String name, int totalHours, Teacher teacher, SchoolClass schoolClass) {
        Course course = new Course();
        course.setName(name);
        course.setTotalHours(totalHours);
        course.setTeacher(teacher);
        course.setSchoolClass(schoolClass);
        return course;
    }
}
