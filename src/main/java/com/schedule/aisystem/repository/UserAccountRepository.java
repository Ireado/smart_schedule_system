package com.schedule.aisystem.repository;

import com.schedule.aisystem.model.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @Override
    @EntityGraph(attributePaths = {"teacher", "student", "student.schoolClass"})
    List<UserAccount> findAll();

    @EntityGraph(attributePaths = {"teacher", "student", "student.schoolClass"})
    Optional<UserAccount> findByAccountNo(String accountNo);
}
