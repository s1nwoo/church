package com.banghwa.service;

import com.banghwa.model.Post;
import com.banghwa.model.PostAttachment;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class PostSchemaUpgradeTest {
    @Test void existingPostsRemainPublicWhenAttachmentSchemaIsAdded() throws Exception {
        String url = "jdbc:h2:mem:post-upgrade-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.execute("create table post (id bigint auto_increment primary key, title varchar(255), writer varchar(255), category varchar(255), content clob, created_date timestamp, updated_date timestamp, deleted boolean, deleted_date timestamp)");
            statement.execute("insert into post (title, category, content, deleted) values ('기존 공지사항', '공지사항', '기존 본문', false)");
        }
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.connection.url", url).applySetting("hibernate.connection.username", "sa")
                .applySetting("hibernate.connection.password", "").applySetting("hibernate.hbm2ddl.auto", "update")
                .applySetting("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .build();
        try (var factory = new MetadataSources(registry).addAnnotatedClass(Post.class).addAnnotatedClass(PostAttachment.class)
                .buildMetadata().buildSessionFactory(); var session = factory.openSession()) {
            var post = session.find(Post.class, 1L);
            assertThat(post.getTitle()).isEqualTo("기존 공지사항");
            assertThat(post.getContent()).isEqualTo("기존 본문");
            assertThat(post.getAdminOnly()).isFalse();
            assertThat(post.getAttachments()).isEmpty();
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }
}
