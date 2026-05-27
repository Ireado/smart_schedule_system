package com.schedule.aisystem.service;

import com.schedule.aisystem.model.Course;
import com.schedule.aisystem.model.Room;
import com.schedule.aisystem.model.SchoolClass;
import com.schedule.aisystem.model.Student;
import com.schedule.aisystem.model.StudentCourseSelection;
import com.schedule.aisystem.model.Teacher;
import com.schedule.aisystem.model.UserAccount;
import com.schedule.aisystem.model.UserRole;
import com.schedule.aisystem.repository.CourseRepository;
import com.schedule.aisystem.repository.RoomRepository;
import com.schedule.aisystem.repository.SchoolClassRepository;
import com.schedule.aisystem.repository.ScheduleEntryRepository;
import com.schedule.aisystem.repository.StudentCourseSelectionRepository;
import com.schedule.aisystem.repository.StudentRepository;
import com.schedule.aisystem.repository.TeacherRepository;
import com.schedule.aisystem.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class UniversitySeedService {

    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final RoomRepository roomRepository;
    private final CourseRepository courseRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;
    private final StudentCourseSelectionRepository selectionRepository;

    public UniversitySeedService(
            TeacherRepository teacherRepository,
            SchoolClassRepository schoolClassRepository,
            RoomRepository roomRepository,
            CourseRepository courseRepository,
            ScheduleEntryRepository scheduleEntryRepository,
            StudentRepository studentRepository,
            UserAccountRepository userAccountRepository,
            StudentCourseSelectionRepository selectionRepository) {
        this.teacherRepository = teacherRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.roomRepository = roomRepository;
        this.courseRepository = courseRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.studentRepository = studentRepository;
        this.userAccountRepository = userAccountRepository;
        this.selectionRepository = selectionRepository;
    }

    @Transactional
    public void resetWithSampleData() {
        scheduleEntryRepository.deleteAllInBatch();
        selectionRepository.deleteAllInBatch();
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
                teacher("T208", "吴涛", "大学物理"),
                teacher("T209", "许倩", "人工智能导论"),
                teacher("T210", "宋浩", "软件工程")));

        List<SchoolClass> classes = schoolClassRepository.saveAll(List.of(
                schoolClass("CS2301", "计算机科学与技术2301", "计算机科学与技术", 28),
                schoolClass("CS2302", "计算机科学与技术2302", "计算机科学与技术", 30),
                schoolClass("SE2301", "软件工程2301", "软件工程", 29),
                schoolClass("SE2302", "软件工程2302", "软件工程", 31),
                schoolClass("AI2301", "人工智能2301", "人工智能", 27),
                schoolClass("NET2301", "网络工程2301", "网络工程", 26)));

        roomRepository.saveAll(List.of(
                room("A101", "教学楼A101", 40),
                room("A102", "教学楼A102", 40),
                room("A201", "教学楼A201", 50),
                room("A202", "教学楼A202", 50),
                room("LAB301", "实验楼301", 36),
                room("LAB302", "实验楼302", 36),
                room("LAB401", "实验楼401", 60),
                room("LAB402", "实验楼402", 60)));

        List<Course> courses = new ArrayList<>();
        for (SchoolClass schoolClass : classes) {
            courses.add(course("高等数学", 40, teachers.get(0), schoolClass));
            courses.add(course("大学英语", 36, teachers.get(1), schoolClass));
            courses.add(course("数据结构", 40, teachers.get(2), schoolClass));
            courses.add(course("数据库原理", 36, teachers.get(4), schoolClass));
            courses.add(course("操作系统", 36, teachers.get(5), schoolClass));
            courses.add(course("计算机网络", 32, teachers.get(3), schoolClass));
            courses.add(course("线性代数", 32, teachers.get(6), schoolClass));
            courses.add(course("大学物理", 32, teachers.get(7), schoolClass));
            courses.add(course("人工智能导论", 32, teachers.get(8), schoolClass));
            courses.add(course("软件工程", 32, teachers.get(9), schoolClass));
        }
        List<Course> savedCourses = courseRepository.saveAll(courses);

        List<Student> students = seedStudents(classes);
        seedTeacherAccounts(teachers);
        seedAdminAccounts();
        seedStudentSelections(students, savedCourses);
    }

    private List<Student> seedStudents(List<SchoolClass> classes) {
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
                student.setMajor(schoolClass.getMajor());
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
        return savedStudents;
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

    private void seedStudentSelections(List<Student> students, List<Course> courses) {
        Random random = new Random(520L);
        List<StudentCourseSelection> selections = new ArrayList<>();

        for (Student student : students) {
            List<Course> classCourses = courses.stream()
                    .filter(course -> course.getSchoolClass().getId().equals(student.getSchoolClass().getId()))
                    .toList();

            for (Course course : classCourses) {
                boolean required = List.of("高等数学", "大学英语", "数据结构", "数据库原理", "操作系统").contains(course.getName());
                boolean chosen = required;
                if (!required) {
                    if ("人工智能".equals(student.getMajor())) {
                        chosen = !"大学物理".equals(course.getName()) || random.nextBoolean();
                    } else {
                        chosen = random.nextDouble() > 0.35;
                    }
                }

                if (chosen) {
                    StudentCourseSelection selection = new StudentCourseSelection();
                    selection.setStudent(student);
                    selection.setCourse(course);
                    selections.add(selection);
                }
            }
        }

        selectionRepository.saveAll(selections);
    }

    private Teacher teacher(String code, String name, String subject) {
        Teacher teacher = new Teacher();
        teacher.setTeacherCode(code);
        teacher.setName(name);
        teacher.setSubject(subject);
        return teacher;
    }

    private SchoolClass schoolClass(String code, String name, String major, int studentCount) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassCode(code);
        schoolClass.setName(name);
        schoolClass.setMajor(major);
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
}
