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

/**
 * Data-access object for {@link Trainee}. Used to talk to {@code
 * TraineeStorage} (our old in-memory Map); now it talks to Hibernate's
 * {@link Session} directly, which is what actually turns these calls into
 * SQL against H2.
 * <p>
 * Every method here calls {@code sessionFactory.getCurrentSession()}
 * instead of {@code openSession()} (compare with {@code
 * TrainingTypeSeeder}, which opens its own session by hand). {@code
 * getCurrentSession()} asks Spring for the Session that's already open for
 * the current transaction -- which means these methods can only run inside
 * a method some Service class marks {@code @Transactional} (that annotation
 * is what actually opens that session in the first place). We haven't
 * added {@code @Transactional} to the Service layer yet (that's a later
 * step), so calling these methods right now would fail with "no session
 * currently bound" -- expected at this point in the migration.
 */
@Repository
public class TraineeDao {

    private static final Logger log = LoggerFactory.getLogger(TraineeDao.class);

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session session() {
        return sessionFactory.getCurrentSession();
    }

    public Trainee create(Trainee trainee) {
        // trainee.user has cascade = ALL (see Trainee.java), so persisting
        // the Trainee automatically persists its brand-new User too --
        // Hibernate inserts both rows, wiring the user_id FK itself.
        session().persist(trainee);
        log.debug("Saved new trainee with id={}", trainee.getId());
        return trainee;
    }

    public Trainee update(Trainee trainee) {
        Long id = trainee.getId();
        if (id == null || session().find(Trainee.class, id) == null) {
            log.warn("Rejected update: no trainee found with id={}", id);
            throw new EntityNotFoundException("Cannot update trainee: no trainee found with id=" + id);
        }
        // merge(): reconciles the (possibly detached) trainee object we
        // were handed with the one Hibernate already knows about, and
        // returns the managed, up-to-date entity -- also cascades into the
        // nested User the same way persist() does.
        Trainee merged = session().merge(trainee);
        log.debug("Updated trainee with id={}", id);
        return merged;
    }

    public void delete(Long id) {
        // Note 7 in the task: deleting a trainee has to hard-delete its
        // trainings too. Training only points AT its Trainee (@ManyToOne),
        // nothing on the Trainee side lists its trainings back for
        // Hibernate to cascade through automatically -- so we do it by
        // hand: a bulk HQL delete for the trainings, then the trainee.
        int deletedTrainings = session()
                .createMutationQuery("delete from Training t where t.trainee.id = :traineeId")
                .setParameter("traineeId", id)
                .executeUpdate();

        Trainee trainee = session().find(Trainee.class, id);
        if (trainee != null) {
            session().remove(trainee);
        }
        log.debug("Deleted trainee with id={} (also removed {} of their trainings)", id, deletedTrainings);
    }

    public Trainee select(Long id) {
        Trainee trainee = session().find(Trainee.class, id);
        log.debug("Selected trainee with id={} -> found={}", id, trainee != null);
        return trainee;
    }

    public Trainee selectByUsername(String username) {
        Trainee trainee = session()
                .createQuery("from Trainee t where t.user.username = :username", Trainee.class)
                .setParameter("username", username)
                .uniqueResultOptional()
                .orElse(null);
        log.debug("Selected trainee by username={} -> found={}", username, trainee != null);
        return trainee;
    }

    public List<Trainee> selectAll() {
        List<Trainee> all = session().createQuery("from Trainee", Trainee.class).list();
        log.debug("Selected all trainees, count={}", all.size());
        return all;
    }

    /**
     * Points 3: "Trainee username and password matching" -- one query that
     * just checks a count instead of loading the whole Trainee, since all
     * we actually need here is yes/no.
     */
    public boolean matchesCredentials(String username, String password) {
        Long count = session()
                .createQuery("select count(t) from Trainee t where t.user.username = :username " +
                        "and t.user.password = :password", Long.class)
                .setParameter("username", username)
                .setParameter("password", password)
                .uniqueResult();
        boolean matches = count != null && count > 0;
        log.debug("Credential check for trainee username={} -> matches={}", username, matches);
        return matches;
    }

    /**
     * Point 7: "Trainee password change". Notice there's no explicit
     * save()/update() call at the end -- once a Session has loaded an
     * entity, that entity is "managed": any change we make to one of its
     * fields is automatically noticed and written to the database when the
     * transaction commits. This is called "dirty checking", and it's a
     * different (and, for a single-field change like this, simpler)
     * mechanism than the persist()/merge() we used in create()/update().
     */
    public void changePassword(Long id, String newPassword) {
        Trainee trainee = session().find(Trainee.class, id);
        if (trainee == null) {
            throw new EntityNotFoundException("Cannot change password: no trainee found with id=" + id);
        }
        trainee.getUser().setPassword(newPassword);
        log.debug("Changed password for trainee id={}", id);
    }

    /**
     * Point 11: "Activate/De-activate trainee". Deliberately NOT
     * idempotent (per the task notes): activating an already-active
     * trainee is rejected instead of silently doing nothing.
     */
    public void activate(Long id) {
        Trainee trainee = session().find(Trainee.class, id);
        if (trainee == null) {
            throw new EntityNotFoundException("Cannot activate: no trainee found with id=" + id);
        }
        if (trainee.isActive()) {
            throw new InvalidProfileStateException("Trainee with id=" + id + " is already active");
        }
        trainee.getUser().setActive(true);
        log.debug("Activated trainee id={}", id);
    }

    public void deactivate(Long id) {
        Trainee trainee = session().find(Trainee.class, id);
        if (trainee == null) {
            throw new EntityNotFoundException("Cannot deactivate: no trainee found with id=" + id);
        }
        if (!trainee.isActive()) {
            throw new InvalidProfileStateException("Trainee with id=" + id + " is already inactive");
        }
        trainee.getUser().setActive(false);
        log.debug("Deactivated trainee id={}", id);
    }

    /**
     * Point 18: "Update Trainee's trainers list". Takes the already-loaded
     * Trainer entities the Service layer resolved (from whatever trainer
     * usernames the caller asked for) and replaces the trainee's whole
     * "trainers" set with them. Reassigning the collection on a managed
     * entity is dirty-checking again -- Hibernate compares the old and new
     * sets and rewrites the trainee_trainer join table to match (deleting
     * rows for trainers that got removed, inserting rows for new ones).
     */
    public Trainee updateTrainersList(Long traineeId, Set<Trainer> trainers) {
        Trainee trainee = session().find(Trainee.class, traineeId);
        if (trainee == null) {
            throw new EntityNotFoundException("Cannot update trainers list: no trainee found with id=" + traineeId);
        }
        trainee.setTrainers(trainers);
        log.debug("Updated trainers list for trainee id={}, now has {} trainer(s)", traineeId, trainers.size());
        return trainee;
    }
}
