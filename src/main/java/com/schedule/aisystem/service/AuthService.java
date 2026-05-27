package com.schedule.aisystem.service;

import com.schedule.aisystem.dto.CurrentUserResponse;
import com.schedule.aisystem.dto.LoginRequest;
import com.schedule.aisystem.dto.LoginResponse;
import com.schedule.aisystem.dto.ViewerOption;
import com.schedule.aisystem.model.UserAccount;
import com.schedule.aisystem.model.UserRole;
import com.schedule.aisystem.repository.SchoolClassRepository;
import com.schedule.aisystem.repository.StudentRepository;
import com.schedule.aisystem.repository.TeacherRepository;
import com.schedule.aisystem.repository.UserAccountRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

        private final UserAccountRepository userAccountRepository;
        private final TeacherRepository teacherRepository;
        private final SchoolClassRepository schoolClassRepository;
        private final StudentRepository studentRepository;

        public AuthService(
                        UserAccountRepository userAccountRepository,
                        TeacherRepository teacherRepository,
                        SchoolClassRepository schoolClassRepository,
                        StudentRepository studentRepository) {
                this.userAccountRepository = userAccountRepository;
                this.teacherRepository = teacherRepository;
                this.schoolClassRepository = schoolClassRepository;
                this.studentRepository = studentRepository;
        }

        public LoginResponse login(LoginRequest request) {
                UserAccount account = loadByAccountNo(request.accountNo());
                if (!account.getPassword().equals(request.password())) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
                }

                validatePrefix(account);
                return new LoginResponse(
                                account.getAccountNo(),
                                account.getDisplayName(),
                                account.getRole().name(),
                                "登录成功");
        }

        @Transactional(readOnly = true)
        public CurrentUserResponse currentUser(String accountNo) {
                UserAccount account = loadByAccountNo(accountNo);

                List<ViewerOption> teacherOptions = account.getRole() == UserRole.ADMIN
                                ? teacherRepository.findAll().stream()
                                                .map(item -> new ViewerOption(item.getId(), item.getTeacherCode(),
                                                                item.getName()))
                                                .toList()
                                : List.of();
                List<ViewerOption> classOptions = account.getRole() == UserRole.ADMIN
                                ? schoolClassRepository.findAll().stream()
                                                .map(item -> new ViewerOption(item.getId(), item.getClassCode(),
                                                                item.getName()))
                                                .toList()
                                : List.of();
                List<ViewerOption> studentOptions = account.getRole() == UserRole.ADMIN
                                ? studentRepository.findAll().stream()
                                                .map(item -> new ViewerOption(item.getId(), item.getStudentNo(),
                                                                item.getName()))
                                                .toList()
                                : List.of();
                List<String> majorOptions = account.getRole() == UserRole.ADMIN
                                ? schoolClassRepository.findAll().stream()
                                                .map(item -> item.getMajor())
                                                .distinct()
                                                .sorted()
                                                .collect(Collectors.toList())
                                : List.of();

                return new CurrentUserResponse(
                                account.getAccountNo(),
                                account.getDisplayName(),
                                account.getRole().name(),
                                account.getTeacher() == null ? null : account.getTeacher().getId(),
                                account.getStudent() == null ? null : account.getStudent().getId(),
                                account.getStudent() == null ? null : account.getStudent().getSchoolClass().getId(),
                                account.getStudent() == null ? null : account.getStudent().getSchoolClass().getName(),
                                account.getStudent() == null ? null : account.getStudent().getMajor(),
                                teacherOptions,
                                classOptions,
                                studentOptions,
                                majorOptions);
        }

        @Transactional(readOnly = true)
        public UserAccount requireUser(String accountNo) {
                return loadByAccountNo(accountNo);
        }

        @Transactional(readOnly = true)
        public UserAccount requireAdmin(String accountNo) {
                UserAccount account = loadByAccountNo(accountNo);
                if (account.getRole() != UserRole.ADMIN) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理员可以执行该操作");
                }
                return account;
        }

        private UserAccount loadByAccountNo(String accountNo) {
                if (accountNo == null || accountNo.isBlank()) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少账号信息");
                }
                UserAccount account = userAccountRepository.findByAccountNo(accountNo)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号不存在"));
                validatePrefix(account);
                return account;
        }

        private void validatePrefix(UserAccount account) {
                char prefix = account.getAccountNo().charAt(0);
                boolean matched = (prefix == '1' && account.getRole() == UserRole.STUDENT)
                                || (prefix == '2' && account.getRole() == UserRole.TEACHER)
                                || (prefix == '3' && account.getRole() == UserRole.ADMIN);
                if (!matched) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "账号前缀与角色不匹配");
                }
        }
}
