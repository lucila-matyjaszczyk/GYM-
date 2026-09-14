package com.gymcrm.service;

import com.gymcrm.aspect.annotation.EnableArgumentLogging;
import com.gymcrm.aspect.annotation.EnableExecutionTimeLogging;
import com.gymcrm.aspect.annotation.EnableReturnValueLogging;
import com.gymcrm.aspect.annotation.RequireAuthentication;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.dao.TrainingTypeDao;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.security.Credentials;
import com.gymcrm.util.UserCredentialsGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for Trainer profiles: create / update / select (no delete
 * on purpose, see TrainerDao), plus authentication, password change and
 * activate/de-activate. Same authentication/transaction conventions as
 * {@link TraineeService} -- see its class comment for the full explanation.
 */
@Service
public class TrainerService {

    private static final Logger log = LoggerFactory.getLogger(TrainerService.class);

    private TrainerDao trainerDao;
    private TrainingTypeDao trainingTypeDao;
    private UserCredentialsGenerator credentialsGenerator;

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Autowired
    public void setTrainingTypeDao(TrainingTypeDao trainingTypeDao) {
        this.trainingTypeDao = trainingTypeDao;
    }

    @Autowired
    public void setCredentialsGenerator(UserCredentialsGenerator credentialsGenerator) {
        this.credentialsGenerator = credentialsGenerator;
    }

    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional
    public Trainer createTrainerProfile(String firstName, String lastName, TrainingType specialization) {
        requireNotBlank(firstName, "firstName");
        requireNotBlank(lastName, "lastName");
        if (specialization == null) {
            throw new IllegalArgumentException("specialization is required");
        }

        Trainer trainer = new Trainer(firstName, lastName, specialization);
        // the generator itself checks both trainees AND trainers for a
        // collision, so we don't build our own "existing usernames" set here
        trainer.setUsername(credentialsGenerator.generateUsername(firstName, lastName));
        trainer.setPassword(credentialsGenerator.generatePassword());
        trainer.setActive(true);

        Trainer created = trainerDao.create(trainer);
        log.info("Created trainer profile: id={}, username={}", created.getId(), created.getUsername());
        return created;
    }

    /**
     * Not one of the 18 numbered points, but needed to make
     * {@code createTrainerProfile} usable at all: {@code Trainer.specialization}
     * is a real {@code @ManyToOne} foreign key now, so a caller (like
     * {@code Application}, or a future UI) needs a way to see the actual,
     * already-persisted {@link TrainingType} rows before it can hand one to
     * {@code createTrainerProfile}. No authentication required, for the same
     * reason {@code createTrainerProfile} itself needs none: this is
     * typically used BEFORE a trainer profile (and its credentials) exist.
     */
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional(readOnly = true)
    public List<TrainingType> getAvailableSpecializations() {
        return trainingTypeDao.selectAll();
    }

    /** Point 4: "Trainer username and password matching", as its own callable check. */
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional(readOnly = true)
    public boolean authenticate(Credentials credentials) {
        return trainerDao.matchesCredentials(credentials.username(), credentials.password());
    }

    @RequireAuthentication
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional
    public Trainer updateTrainerProfile(Credentials credentials, Trainer trainer) {
        requireNotBlank(trainer.getFirstName(), "firstName");
        requireNotBlank(trainer.getLastName(), "lastName");
        Trainer updated = trainerDao.update(trainer);
        log.info("Updated trainer profile: id={}", updated.getId());
        return updated;
    }

    @RequireAuthentication
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional(readOnly = true)
    public Trainer selectTrainerProfile(Credentials credentials, Long trainerId) {
        return trainerDao.select(trainerId);
    }

    /** Point 5: "Select Trainer profile by username". */
    @RequireAuthentication
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional(readOnly = true)
    public Trainer selectTrainerProfileByUsername(Credentials credentials, String username) {
        return trainerDao.selectByUsername(username);
    }

    /** Point 8: "Trainer password change". */
    @RequireAuthentication
    @EnableArgumentLogging
    @EnableExecutionTimeLogging
    @Transactional
    public void changeTrainerPassword(Credentials credentials, Long trainerId, String newPassword) {
        requireNotBlank(newPassword, "newPassword");
        trainerDao.changePassword(trainerId, newPassword);
        log.info("Changed password for trainer id={}", trainerId);
    }

    /** Point 12: "Activate/De-activate trainer". */
    @RequireAuthentication
    @EnableArgumentLogging
    @EnableExecutionTimeLogging
    @Transactional
    public void activateTrainer(Credentials credentials, Long trainerId) {
        trainerDao.activate(trainerId);
        log.info("Activated trainer id={}", trainerId);
    }

    @RequireAuthentication
    @EnableArgumentLogging
    @EnableExecutionTimeLogging
    @Transactional
    public void deactivateTrainer(Credentials credentials, Long trainerId) {
        trainerDao.deactivate(trainerId);
        log.info("Deactivated trainer id={}", trainerId);
    }

    private void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
