package com.banghwa.repository;

import com.banghwa.model.BibleProgress;
import com.banghwa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BibleProgressRepository extends JpaRepository<BibleProgress, Long> {
    Optional<BibleProgress> findByUserAndBookCode(User user, String bookCode);
}
