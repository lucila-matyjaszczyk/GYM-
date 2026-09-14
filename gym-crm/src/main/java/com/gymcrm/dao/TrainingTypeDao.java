package com.gymcrm.dao;

import com.gymcrm.domain.TrainingType;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data-access object for the small, fixed {@link TrainingType} catalog (see
 * {@code TrainingTypeSeeder}, which fills it once at startup with "Fitness",
 * "Yoga", "Zumba", "Stretching" and "Resistance").
 * <p>
 * This one is new, and not one of the 18 numbered points in the task -- it
 * exists because {@code Trainer.specialization} is now a real
 * {@code @ManyToOne} foreign key to a persisted row (not just a name), so
 * something has to let a caller look up an actual, already-saved
 * {@link TrainingType} before it can be handed to
 * {@code createTrainerProfile}. Read-only on purpose: the application never
 * creates/updates/deletes a training type itself.
 */
@Repository
public class TrainingTypeDao {

    private static final Logger log = LoggerFactory.getLogger(TrainingTypeDao.class);

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session session() {
        return sessionFactory.getCurrentSession();
    }

    public List<TrainingType> selectAll() {
        List<TrainingType> all = session().createQuery("from TrainingType", TrainingType.class).list();
        log.debug("Selected all training types, count={}", all.size());
        return all;
    }
}
