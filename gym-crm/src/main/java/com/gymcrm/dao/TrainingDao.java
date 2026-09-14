package com.gymcrm.dao;

import com.gymcrm.domain.Training;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-access object for {@link Training}, now backed by Hibernate. Only
 * create/select on purpose: the spec does not ask for update or delete of
 * individual trainings (deletion only ever happens indirectly, as a side
 * effect of deleting a trainee -- see {@code TraineeDao.delete}).
 */
@Repository
public class TrainingDao {

    private static final Logger log = LoggerFactory.getLogger(TrainingDao.class);

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session session() {
        return sessionFactory.getCurrentSession();
    }

    public Training create(Training training) {
        session().persist(training);
        log.debug("Saved new training with id={}", training.getId());
        return training;
    }

    public Training select(Long id) {
        Training training = session().find(Training.class, id);
        log.debug("Selected training with id={} -> found={}", id, training != null);
        return training;
    }

    public List<Training> selectAll() {
        List<Training> all = session().createQuery("from Training", Training.class).list();
        log.debug("Selected all trainings, count={}", all.size());
        return all;
    }

    /**
     * Point 14 of the task: a trainee's trainings, filtered by whichever of
     * the four criteria were actually given (any/all of them can be
     * {@code null}/blank to mean "don't filter on this").
     * <p>
     * Built as one HQL string assembled piece by piece -- we start from the
     * mandatory "belongs to this trainee" condition, then tack on one more
     * {@code and ...} for each criterion that was actually passed in, so
     * the final query only ever asks the database for exactly the
     * conditions that matter for this particular call.
     */
    public List<Training> selectByTraineeUsernameAndCriteria(String traineeUsername, LocalDate fromDate,
                                                               LocalDate toDate, String trainerName,
                                                               String trainingTypeName) {
        StringBuilder hql = new StringBuilder("from Training t where t.trainee.user.username = :traineeUsername");
        Map<String, Object> params = new HashMap<>();
        params.put("traineeUsername", traineeUsername);

        if (fromDate != null) {
            hql.append(" and t.trainingDate >= :fromDate");
            params.put("fromDate", fromDate);
        }
        if (toDate != null) {
            hql.append(" and t.trainingDate <= :toDate");
            params.put("toDate", toDate);
        }
        if (trainerName != null && !trainerName.isBlank()) {
            // "name" isn't specified as first/last in the task, so we match
            // loosely (contains, case-insensitive) against either one.
            hql.append(" and (lower(t.trainer.user.firstName) like :trainerName" +
                    " or lower(t.trainer.user.lastName) like :trainerName)");
            params.put("trainerName", "%" + trainerName.toLowerCase() + "%");
        }
        if (trainingTypeName != null && !trainingTypeName.isBlank()) {
            hql.append(" and t.trainingType.trainingTypeName = :trainingTypeName");
            params.put("trainingTypeName", trainingTypeName);
        }

        Query<Training> query = session().createQuery(hql.toString(), Training.class);
        params.forEach(query::setParameter);
        List<Training> result = query.list();
        log.debug("Selected {} trainings for trainee username={} with criteria", result.size(), traineeUsername);
        return result;
    }

    /**
     * Point 15: the same idea, from the trainer's side (criteria are: from
     * date, to date, trainee name -- no training type filter here, per the
     * task).
     */
    public List<Training> selectByTrainerUsernameAndCriteria(String trainerUsername, LocalDate fromDate,
                                                               LocalDate toDate, String traineeName) {
        StringBuilder hql = new StringBuilder("from Training t where t.trainer.user.username = :trainerUsername");
        Map<String, Object> params = new HashMap<>();
        params.put("trainerUsername", trainerUsername);

        if (fromDate != null) {
            hql.append(" and t.trainingDate >= :fromDate");
            params.put("fromDate", fromDate);
        }
        if (toDate != null) {
            hql.append(" and t.trainingDate <= :toDate");
            params.put("toDate", toDate);
        }
        if (traineeName != null && !traineeName.isBlank()) {
            hql.append(" and (lower(t.trainee.user.firstName) like :traineeName" +
                    " or lower(t.trainee.user.lastName) like :traineeName)");
            params.put("traineeName", "%" + traineeName.toLowerCase() + "%");
        }

        Query<Training> query = session().createQuery(hql.toString(), Training.class);
        params.forEach(query::setParameter);
        List<Training> result = query.list();
        log.debug("Selected {} trainings for trainer username={} with criteria", result.size(), trainerUsername);
        return result;
    }
}
