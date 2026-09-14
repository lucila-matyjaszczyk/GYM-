package com.gymcrm.dao;

import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.exception.InvalidProfileStateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data-access object for {@link Trainer}, now backed by Hibernate. No
 * delete here on purpose: the spec only asks for create/update/select for
 * trainers (trainers get deactivated, never removed).
 */
@Repository
public class TrainerDao {

    private static final Logger log = LoggerFactory.getLogger(TrainerDao.class);

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session session() {
        return sessionFactory.getCurrentSession();
    }

    public Trainer create(Trainer trainer) {
        // trainer.user has cascade = ALL too, same reasoning as Trainee.
        session().persist(trainer);
        log.debug("Saved new trainer with id={}", trainer.getId());
        return trainer;
    }

    public Trainer update(Trainer trainer) {
        Long id = trainer.getId();
        if (id == null || session().find(Trainer.class, id) == null) {
            log.warn("Rejected update: no trainer found with id={}", id);
            throw new EntityNotFoundException("Cannot update trainer: no trainer found with id=" + id);
        }
        Trainer merged = session().merge(trainer);
        log.debug("Updated trainer with id={}", id);
        return merged;
    }

    public Trainer select(Long id) {
        Trainer trainer = session().find(Trainer.class, id);
        log.debug("Selected trainer with id={} -> found={}", id, trainer != null);
        return trainer;
    }

    public Trainer selectByUsername(String username) {
        Trainer trainer = session()
                .createQuery("from Trainer t where t.user.username = :username", Trainer.class)
                .setParameter("username", username)
                .uniqueResultOptional()
                .orElse(null);
        log.debug("Selected trainer by username={} -> found={}", username, trainer != null);
        return trainer;
    }

    public List<Trainer> selectAll() {
        List<Trainer> all = session().createQuery("from Trainer", Trainer.class).list();
        log.debug("Selected all trainers, count={}", all.size());
        return all;
    }

    /** Point 4: "Trainer username and password matching" -- same idea as TraineeDao. */
    public boolean matchesCredentials(String username, String password) {
        Long count = session()
                .createQuery("select count(t) from Trainer t where t.user.username = :username " +
                        "and t.user.password = :password", Long.class)
                .setParameter("username", username)
                .setParameter("password", password)
                .uniqueResult();
        boolean matches = count != null && count > 0;
        log.debug("Credential check for trainer username={} -> matches={}", username, matches);
        return matches;
    }

    /** Point 8: "Trainer password change" -- relies on dirty checking, see TraineeDao.changePassword. */
    public void changePassword(Long id, String newPassword) {
        Trainer trainer = session().find(Trainer.class, id);
        if (trainer == null) {
            throw new EntityNotFoundException("Cannot change password: no trainer found with id=" + id);
        }
        trainer.getUser().setPassword(newPassword);
        log.debug("Changed password for trainer id={}", id);
    }

    /** Point 12: "Activate/De-activate trainer" -- not idempotent, same as TraineeDao. */
    public void activate(Long id) {
        Trainer trainer = session().find(Trainer.class, id);
        if (trainer == null) {
            throw new EntityNotFoundException("Cannot activate: no trainer found with id=" + id);
        }
        if (trainer.isActive()) {
            throw new InvalidProfileStateException("Trainer with id=" + id + " is already active");
        }
        trainer.getUser().setActive(true);
        log.debug("Activated trainer id={}", id);
    }

    public void deactivate(Long id) {
        Trainer trainer = session().find(Trainer.class, id);
        if (trainer == null) {
            throw new EntityNotFoundException("Cannot deactivate: no trainer found with id=" + id);
        }
        if (!trainer.isActive()) {
            throw new InvalidProfileStateException("Trainer with id=" + id + " is already inactive");
        }
        trainer.getUser().setActive(false);
        log.debug("Deactivated trainer id={}", id);
    }

    /**
     * Point 17: "Get trainers list that not assigned on trainee by
     * trainee's username". We load the trainee, read the ids of the
     * trainers it already has (accessing trainee.getTrainers() triggers
     * Hibernate to lazily fetch that collection right here, since we're
     * still inside the same open session), then ask for every trainer
     * whose id is NOT in that set.
     */
    public List<Trainer> selectNotAssignedToTraineeUsername(String traineeUsername) {
        Trainee trainee = session()
                .createQuery("from Trainee t where t.user.username = :username", Trainee.class)
                .setParameter("username", traineeUsername)
                .uniqueResultOptional()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cannot list unassigned trainers: no trainee found with username=" + traineeUsername));

        Set<Long> assignedTrainerIds = trainee.getTrainers().stream()
                .map(Trainer::getId)
                .collect(Collectors.toSet());

        List<Trainer> result;
        if (assignedTrainerIds.isEmpty()) {
            // "not in ()" isn't valid HQL/SQL when the collection is empty
            // -- if nothing is assigned yet, every trainer qualifies.
            result = session().createQuery("from Trainer", Trainer.class).list();
        } else {
            result = session()
                    .createQuery("from Trainer tr where tr.id not in :assignedIds", Trainer.class)
                    .setParameter("assignedIds", assignedTrainerIds)
                    .list();
        }
        log.debug("Selected {} trainer(s) not assigned to trainee username={}", result.size(), traineeUsername);
        return result;
    }
}
