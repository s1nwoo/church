package com.banghwa.service;

import com.banghwa.model.SermonVideo;
import com.banghwa.model.WorshipType;
import com.banghwa.repository.SermonVideoRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SermonWorshipTypeTest {
    private SessionFactory factory;
    private EntityManager em;
    private SermonVideoRepository repository;
    private SermonVideoServiceImpl service;

    @BeforeEach
    void upgradeExistingDatabase() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        // Start with the previous schema and a real row, then run the same schema update as the app.
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("""
                    create table sermon_video (
                      id bigint auto_increment primary key,
                      youtube_url varchar(255) not null, preacher varchar(100) not null,
                      sermon_date date not null, bible_text varchar(100), content text,
                      created_date timestamp, updated_date timestamp,
                      deleted boolean, deleted_date timestamp
                    )
                    """);
            statement.execute("""
                    insert into sermon_video (youtube_url, preacher, sermon_date, content, deleted)
                    values ('https://youtu.be/legacy00001', '기존 설교자', '2026-10-18', '기존 설교', false)
                    """);
        }
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.connection.url", url)
                .applySetting("hibernate.connection.username", "sa")
                .applySetting("hibernate.connection.password", "")
                .applySetting("hibernate.hbm2ddl.auto", "update")
                .build();
        factory = new MetadataSources(registry).addAnnotatedClass(SermonVideo.class)
                .buildMetadata().buildSessionFactory();
        em = factory.createEntityManager();
        em.getTransaction().begin();
        repository = new JpaRepositoryFactory(em).getRepository(SermonVideoRepository.class);
        service = new SermonVideoServiceImpl(repository, mock(BibleLocalService.class));
    }

    @AfterEach
    void closeDatabase() {
        if (em != null) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close();
        }
        if (factory != null) factory.close();
    }

    @Test
    void schemaUpdatePreservesExistingSermonsAsSunday() {
        SermonVideo old = repository.findAll().get(0);
        assertThat(old.getWorshipType()).isEqualTo(WorshipType.SUNDAY);
        assertThat(old.getContent()).isEqualTo("기존 설교");
        assertThat(old.getYoutubeUrl()).isEqualTo("https://youtu.be/legacy00001");
        assertThat(old.getSermonDate()).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void filtersBeforePaginationAndKeepsBothServicesOnSameDate() {
        save("오후 말씀 1", WorshipType.AFTERNOON, false);
        save("오후 말씀 2", WorshipType.AFTERNOON, false);
        save("오후 삭제 말씀", WorshipType.AFTERNOON, true);
        var page = service.searchSermons("말씀", PageRequest.of(0, 1,
                Sort.by(Sort.Direction.DESC, "sermonDate", "id")), false, WorshipType.AFTERNOON);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(SermonVideo::getContent).containsExactly("오후 말씀 2");
        assertThat(service.searchSermons("", PageRequest.of(0, 10), false, WorshipType.SUNDAY)
                .getTotalElements()).isEqualTo(1);
        assertThat(service.searchSermons("", PageRequest.of(0, 10), true, null)
                .getTotalElements()).isEqualTo(4);
        assertThat(repository.existsBySermonDateAndWorshipTypeAndDeletedFalse(
                LocalDate.of(2026, 10, 18), WorshipType.AFTERNOON)).isTrue();
    }

    @Test
    void adminCanReclassifyAnExistingSermon() {
        var old = repository.findAll().get(0);
        var updated = SermonVideo.builder().youtubeUrl(old.getYoutubeUrl())
                .preacher(old.getPreacher()).sermonDate(old.getSermonDate())
                .content(old.getContent()).deleted(false).worshipType(WorshipType.AFTERNOON).build();
        service.updateSermon(old.getId(), updated);
        em.flush();
        em.clear();
        assertThat(repository.findById(old.getId()).orElseThrow().getWorshipType())
                .isEqualTo(WorshipType.AFTERNOON);
        assertThat(service.searchSermons("", PageRequest.of(0, 10), false, WorshipType.SUNDAY)).isEmpty();
    }

    @Test
    void omittedOrNullTypeDefaultsToSundayOnCreate() {
        var sermon = SermonVideo.builder().youtubeUrl("https://youtu.be/default0001")
                .preacher("설교자").sermonDate(LocalDate.of(2026, 10, 18)).deleted(false).build();
        assertThat(sermon.getWorshipType()).isEqualTo(WorshipType.SUNDAY);
        sermon.setWorshipType(null);
        service.createSermon(sermon);
        em.flush();
        em.clear();
        assertThat(repository.findById(sermon.getId()).orElseThrow().getWorshipType())
                .isEqualTo(WorshipType.SUNDAY);
    }

    private void save(String title, WorshipType type, boolean deleted) {
        repository.save(SermonVideo.builder().content(title).worshipType(type).deleted(deleted)
                .youtubeUrl("https://youtu.be/" + UUID.randomUUID()).preacher("설교자")
                .sermonDate(LocalDate.of(2026, 10, 18)).build());
    }
}
