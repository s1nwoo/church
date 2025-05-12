// src/main/java/com/banghwa/repository/PostRepository.java
package com.banghwa.repository;

import com.banghwa.model.Post;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository
        extends JpaRepository<Post, Long>,
        JpaSpecificationExecutor<Post> {}
