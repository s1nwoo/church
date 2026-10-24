// src/main/java/com/banghwa/repository/PostRepository.java
package com.banghwa.repository;

import com.banghwa.model.Post;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository
        extends JpaRepository<Post, Long>,
        JpaSpecificationExecutor<Post> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    java.util.Optional<Post> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
