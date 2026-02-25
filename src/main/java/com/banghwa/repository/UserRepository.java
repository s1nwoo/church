package com.banghwa.repository;

import com.banghwa.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);

    // 삭제되지 않은 회원 조회
    Page<User> findByDeletedFalse(Pageable pageable);

    // 회원 검색 (이름, 아이디, 이메일)
    @Query("SELECT u FROM User u WHERE " +
            "(u.name LIKE %:keyword% OR u.username LIKE %:keyword% OR u.email LIKE %:keyword%) " +
            "AND (:includeDeleted = true OR u.deleted = false)")
    Page<User> searchUsers(@Param("keyword") String keyword,
                           @Param("includeDeleted") boolean includeDeleted,
                           Pageable pageable);
}