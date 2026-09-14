package com.gymcrm.config;

import com.gymcrm.domain.TrainingType;
import jakarta.annotation.PostConstruct;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Makes sure the fixed catalog of training types exists in the database.
 * Runs once, right when the application starts, and only inserts rows if
 * the table is completely empty -- so restarting the app never creates
 * duplicates.
 * <p>
 * This replaces what {@code StoragePreloadBeanPostProcessor} used to do for
 * the in-memory storages, but it's built differently on purpose:
 * {@code BeanPostProcessor} exists to intercept and tweak EVERY bean as
 * Spring creates it (that's why we needed it before, to reach into
 * TraineeStorage/TrainerStorage/TrainingStorage specifically as they got
 * built). Here we don't need to intercept anything else's construction --
 * we just want "run this once, after my own dependencies are ready" --
 * which is exactly what {@code @PostConstruct} is for: a much simpler tool
 * for a much simpler job.
 */
@Component
public class TrainingTypeSeeder {

    private static final Logger log = LoggerFactory.getLogger(TrainingTypeSeeder.class);

    private static final List<String> FIXED_TRAINING_TYPES =
            List.of("Fitness", "Yoga", "Zumba", "Stretching", "Resistance");

    private final SessionFactory sessionFactory;

    @Autowired
    public TrainingTypeSeeder(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @PostConstruct
    public void seedIfEmpty() {
        // A Session is one single conversation with the database -- we open
        // one by hand here (instead of relying on @Transactional, which
        // only works on Spring-managed Service methods) because this method
        // runs during application startup, before any request-like call
        // is happening.
        try (Session session = sessionFactory.openSession()) {
            Long existingCount = session
                    .createQuery("select count(t) from TrainingType t", Long.class)
                    .uniqueResult();

            if (existingCount != null && existingCount > 0) {
                log.info("Training types already present ({}), skipping seed.", existingCount);
                return;
            }

            Transaction transaction = session.beginTransaction();
            for (String name : FIXED_TRAINING_TYPES) {
                session.persist(new TrainingType(name));
            }
            transaction.commit();
            log.info("Seeded {} fixed training types: {}", FIXED_TRAINING_TYPES.size(), FIXED_TRAINING_TYPES);
        }
    }
}
