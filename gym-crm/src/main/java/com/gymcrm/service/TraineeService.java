package com.gymcrm.service;

import com.gymcrm.aspect.annotation.EnableArgumentLogging;
import com.gymcrm.aspect.annotation.EnableExecutionTimeLogging;
import com.gymcrm.aspect.annotation.EnableReturnValueLogging;
import com.gymcrm.aspect.annotation.RequireAuthentication;
import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.security.Credentials;
import com.gymcrm.util.UserCredentialsGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Business logic for Trainee profiles: create / update / delete / select,
 * plus (new in this module) authentication, password change,
 * activate/de-activate, and managing a trainee's assigned trainers.
 * <p>
 * Every method except {@code createTraineeProfile} and {@code authenticate}
 * is annotated {@code @RequireAuthentication} and takes a {@code
 * Credentials} as its first parameter -- {@code AuthenticationAspect}
 * checks it before the method body ever runs (see note 2 of the task).
 * Every method that touches the database (basically all of them) is
 * {@code @Transactional}, since our Hibernate-backed DAOs need an open
 * session/transaction to work at all.
 */
@Service
public class TraineeService {

    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private TraineeDao traineeDao;
    private TrainerDao trainerDao;
    private UserCredentialsGenerator credentialsGenerator;

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Autowired
    public void setCredentialsGenerator(UserCredentialsGenerator credentialsGenerator) {
        this.credentialsGenerator = credentialsGenerator;
    }

    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional
    public Trainee createTraineeProfile(String firstName, String lastName, LocalDate dateOfBirth, String address) {
        requireNotBlank(firstName, "firstName");
        requireNotBlank(lastName, "lastName");

        Trainee trainee = new Trainee(firstName, lastName);
        // the generator itself checks both trainees AND trainers for a
        // collision, so we don't build our own "existing usernames" set here
        trainee.setUsername(credentialsGenerator.generateUsername(firstName, lastName));
        trainee.setPassword(credentialsGenerator.generatePassword());
        trainee.setActive(true);
        trainee.setDateOfBirth(dateOfBirth);
        trainee.setAddress(address);

        Trainee created = traineeDao.create(trainee);
        log.info("Created trainee profile: id={}, username={}", created.getId(), created.getUsername());
        return created;
    }

    /** Point 3: "Trainee username and password matching", as its own callable check (e.g. a login screen). */
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional(readOnly = true)
    public boolean authenticate(Credentials credentials) {
        return traineeDao.matchesCredentials(credentials.username(), credentials.password());
    }

    @RequireAuthentication
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional
    public Trainee updateTraineeProfile(Credentials credentials, Trainee trainee) {
        requireNotBlank(trainee.getFirstName(), "firstName");
        requireNotBlank(trainee.getLastName(), "lastName");
        Trainee updated = traineeDao.update(trainee);
        log.info("Updated trainee profile: id={}", updated.getId());
        return updated;
    }

    @RequireAuthentication
    @EnableArgumentLogging
    @EnableExecutionTimeLogging
    @Transactional
    public void deleteTraineeProfile(Credentials credentials, Long traineeId) {
        traineeDao.delete(traineeId);
        log.info("Deleted trainee profile: id={}", traineeId);
    }

    @RequireAuthentication
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional(readOnly = true)
    public Trainee selectTraineeProfile(Credentials credentials, Long traineeId) {
        return traineeDao.select(traineeId);
    }

    /** Point 6: "Select Trainee profile by username". */
    @RequireAuthentication
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional(readOnly = true)
    public Trainee selectTraineeProfileByUsername(Credentials credentials, String username) {
        return traineeDao.selectByUsername(username);
    }

    /** Point 7: "Trainee password change". */
    @RequireAuthentication
    @EnableArgumentLogging
    @EnableExecutionTimeLogging
    @Transactional
    public void changeTraineePassword(Credentials credentials, Long traineeId, String newPassword) {
        requireNotBlank(newPassword, "newPassword");
        traineeDao.changePassword(traineeId, newPassword);
        log.info("Changed password for trainee id={}", traineeId);
    }

    /** Point 11: "Activate/De-activate trainee". */
    @RequireAuthentication
    @EnableArgumentLogging
    @EnableExecutionTimeLogging
    @Transactional
    public void activateTrainee(Credentials credentials, Long traineeId) {
        traineeDao.activate(traineeId);
        log.info("Activated trainee id={}", traineeId);
    }

    @RequireAuthentication
    @EnableArgumentLogging
    @EnableExecutionTimeLogging
    @Transactional
    public void deactivateTrainee(Credentials credentials, Long traineeId) {
        traineeDao.deactivate(traineeId);
        log.info("Deactivated trainee id={}", traineeId);
    }

    /** Point 17: "Get trainers list that not assigned on trainee by trainee's username". */
    @RequireAuthentication
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional(readOnly = true)
    public List<Trainer> getTrainersNotAssigned(Credentials credentials, String traineeUsername) {
        return trainerDao.selectNotAssignedToTraineeUsername(traineeUsername);
    }

    /** Point 18: "Update Trainee's trainers list" -- resolves each username into a real Trainer first. */
    @RequireAuthentication
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional
    public Trainee updateTraineeTrainers(Credentials credentials, Long traineeId, List<String> trainerUsernames) {
        Set<Trainer> trainers = new HashSet<>();
        for (String username : trainerUsernames) {
            Trainer trainer = trainerDao.selectByUsername(username);
            if (trainer == null) {
                throw new EntityNotFoundException("Cannot update trainers list: no trainer found with username=" + username);
            }
            trainers.add(trainer);
        }
        Trainee updated = traineeDao.updateTrainersList(traineeId, trainers);
        log.info("Updated trainers list for trainee id={}, now has {} trainer(s)", traineeId, trainers.size());
        return updated;
    }

    private void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
