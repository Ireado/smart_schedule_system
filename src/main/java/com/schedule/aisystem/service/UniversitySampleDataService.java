package com.schedule.aisystem.service;

import com.schedule.aisystem.model.Course;
import com.schedule.aisystem.model.Room;
import com.schedule.aisystem.model.SchoolClass;
import com.schedule.aisystem.model.Student;
import com.schedule.aisystem.model.Teacher;
import com.schedule.aisystem.model.UserAccount;
import com.schedule.aisystem.model.UserRole;
import com.schedule.aisystem.repository.CourseRepository;
import com.schedule.aisystem.repository.RoomRepository;
import com.schedule.aisystem.repository.SchoolClassRepository;
import com.schedule.aisystem.repository.ScheduleEntryRepository;
import com.schedule.aisystem.repository.StudentRepository;
import com.schedule.aisystem.repository.TeacherRepository;
import com.schedule.aisystem.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class UniversitySampleDataService {

    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final RoomRepository roomRepository;
    private final CourseRepository courseRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;

    public UniversitySampleDataService(
            TeacherRepository teacherRepository,
            SchoolClassRepository schoolClassRepository,
            RoomRepository roomRepository,
            CourseRepository courseRepository,
            ScheduleEntryRepository scheduleEntryRepository,
            StudentRepository studentRepository,
            UserAccountRepository userAccountRepository) {
        this.teacherRepository = teacherRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.roomRepository = roomRepository;
        this.courseRepository = courseRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.studentRepository = studentRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public void resetWithSampleData() {
        scheduleEntryRepository.deleteAllInBatch();
        userAccountRepository.deleteAllInBatch();
        studentRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        teacherRepository.deleteAllInBatch();
        schoolClassRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();

        List<Teacher> teachers = teacherRepository.saveAll(List.of(
                teacher("T201", "周明", "高等数学"),
                teacher("T202", "李楠", "大学英语"),
                teacher("T203", "陈博", "数据结构"),
                teacher("T204", "王欣", "计算机网络"),
                teacher("T205", "刘洋", "数据库原理"),
                teacher("T206", "赵晴", "操作系统"),
                teacher("T207", "孙悦", "线性代数"),
                teacher("T208", "吴涛", "大学物理")));

        List<SchoolClass> classes = schoolClassRepository.saveAll(List.of(
                schoolClass("CS2301", "计算机科学与技术2301", 32),
                schoolClass("CS2302", "计算机科学与技术2302", 31),
                schoolClass("SE2301", "软件工程2301", 34),
                schoolClass("SE2302", "软件工程2302", 33),
                schoolClass("AI2301", "人工智能2301", 30),
                schoolClass("NET2301", "网络工程2301", 29)));

        roomRepository.saveAll(List.of(
                room("A101", "教学楼A101", 40),
                room("A102", "教学楼A102", 40),
                room("A201", "教学楼A201", 45),
                room("A202", "教学楼A202", 45),
                room("LAB301", "实验楼301", 36),
                room("LAB302", "实验楼302", 36),
                room("LAB401", "实验楼401", 50)));

        List<Course> courses = new ArrayList<>();
        for (SchoolClass schoolClass : classes) {
            courses.add(course("高等数学", 40, teachers.get(0), schoolClass));
            courses.add(course("大学英语", 36, teachers.get(1), schoolClass));
            courses.add(course("数据结构", 40, teachers.get(2), schoolClass));
            courses.add(course("计算机网络", 36, teachers.get(3), schoolClass));
            courses.add(course("数据库原理", 36, teachers.get(4), schoolClass));
            courses.add(course("操作系统", 36, teachers.get(5), schoolClass));
            courses.add(course("线性代数", 32, teachers.get(6), schoolClass));
            courses.add(course("大学物理", 32, teachers.get(7), schoolClass));
        }
        courseRepository.saveAll(courses);

        seedStudentAccounts(classes);
        seedTeacherAccounts(teachers);
        seedAdminAccounts();
    }

    private void seedStudentAccounts(List<SchoolClass> classes) {
        List<Student> students = new ArrayList<>();
        List<UserAccount> accounts = new ArrayList<>();
        Random random = new Random(2301L);
        String[] surnames = { "陈", "林", "黄", "周", "徐", "朱", "胡", "郭", "何", "高", "梁", "郑" };
        String[] givenNames = { "宇轩", "梓涵", "嘉宁", "思远", "浩然", "若溪", "子墨", "依晨", "俊驰", "欣怡" };
        int sequence = 100000;

        for (SchoolClass schoolClass : classes) {
            for (int i = 0; i < schoolClass.getStudentCount(); i++) {
                Student student = new Student();
                student.setStudentNo(String.valueOf(sequence++));
                student.setName(
                        surnames[random.nextInt(surnames.length)] + givenNames[random.nextInt(givenNames.length)]);
                student.setMajor(majorOfClass(schoolClass.getClassCode()));
                student.setGrade("2023级");
                student.setSchoolClass(schoolClass);
                students.add(student);
            }
        }

        List<Student> savedStudents = studentRepository.saveAll(students);
        for (Student student : savedStudents) {
            accounts.add(account(student.getStudentNo(), student.getName(), UserRole.STUDENT, null, student));
        }
        userAccountRepository.saveAll(accounts);
    }

    private void seedTeacherAccounts(List<Teacher> teachers) {
        List<UserAccount> accounts = new ArrayList<>();
        int sequence = 200000;
        for (Teacher teacher : teachers) {
            accounts.add(account(String.valueOf(sequence++), teacher.getName(), UserRole.TEACHER, teacher, null));
        }
        userAccountRepository.saveAll(accounts);
    }

    private void seedAdminAccounts() {
        userAccountRepository.saveAll(List.of(
                account("300001", "教务管理员-张老师", UserRole.ADMIN, null, null),
                account("300002", "系统管理员-李老师", UserRole.ADMIN, null, null)));
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

    private UserAccount account(String accountNo, String displayName, UserRole role, Teacher teacher, Student student) {
        UserAccount account = new UserAccount();
        account.setAccountNo(accountNo);
        account.setPassword("000000");
        account.setDisplayName(displayName);
        account.setRole(role);
        account.setTeacher(teacher);
        account.setStudent(student);
        return account;
    }

    private String majorOfClass(String classCode) {
        if (classCode.startsWith("CS")) {
            return "计算机科学与技术";
        }
        if (classCode.startsWith("SE")) {
            return "软件工程";
        }
        if (classCode.startsWith("AI")) {
            return "人工智能";
        }
        return "网络工程";
    }
}
