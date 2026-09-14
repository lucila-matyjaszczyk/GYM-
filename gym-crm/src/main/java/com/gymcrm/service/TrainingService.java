package com.gymcrm.service;

import com.gymcrm.aspect.annotation.EnableArgumentLogging;
import com.gymcrm.aspect.annotation.EnableExecutionTimeLogging;
import com.gymcrm.aspect.annotation.EnableReturnValueLogging;
import com.gymcrm.aspect.annotation.RequireAuthentication;
import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.dao.TrainingDao;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.security.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for Training records: create / select (no update or
 * delete on purpose, see TrainingDao -- a training only ever disappears as
 * a side effect of deleting its trainee), plus (new in this module) the two
 * criteria-based searches (points 14/15).
 * <p>
 * {@code createTraining} now takes trainee/trainer USERNAMES instead of a
 * pre-built {@code Training} with raw FK ids -- it resolves each username
 * to a real {@link Trainee}/{@link Trainer} itself (that's the "does this
 * id/username actually exist?" validation from before, just phrased around
 * usernames now that {@code Training} holds real object references instead
 * of {@code Long}s).
 */
@Service
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private TrainingDao trainingDao;
    private TraineeDao traineeDao;
    private TrainerDao trainerDao;

    @Autowired
    public void setTrainingDao(TrainingDao trainingDao) {
        this.trainingDao = trainingDao;
    }

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    /** Point 16: "Add training". Requires authentication -- this isn't a profile creation. */
    @RequireAuthentication
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional
    public Training createTraining(Credentials credentials, String traineeUsername, String trainerUsername,
                                    String trainingName, TrainingType trainingType, LocalDate trainingDate,
                                    Integer trainingDuration) {
        requireNotBlank(trainingName, "trainingName");
        if (trainingType == null) {
            throw new IllegalArgumentException("trainingType is required");
        }
        if (trainingDate == null) {
            throw new IllegalArgumentException("trainingDate is required");
        }
        if (trainingDuration == null) {
            throw new IllegalArgumentException("trainingDuration is required");
        }

        Trainee trainee = traineeDao.selectByUsername(traineeUsername);
        if (trainee == null) {
            log.warn("Rejected training creation: no trainee found with username={}", traineeUsername);
            throw new EntityNotFoundException("Cannot create training: no trainee found with username=" + traineeUsername);
        }
        Trainer trainer = trainerDao.selectByUsername(trainerUsername);
        if (trainer == null) {
            log.warn("Rejected training creation: no trainer found with username={}", trainerUsername);
            throw new EntityNotFoundException("Cannot create training: no trainer found with username=" + trainerUsername);
        }

        Training training = new Training(trainee, trainer, trainingName, trainingType, trainingDate, trainingDuration);
        Training created = trainingDao.create(training);
        log.info("Created training: id={}, traineeUsername={}, trainerUsername={}",
                created.getId(), traineeUsername, trainerUsername);
        return created;
    }

    @RequireAuthentication
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional(readOnly = true)
    public Training selectTraining(Credentials credentials, Long trainingId) {
        return trainingDao.select(trainingId);
    }

    /** Point 14: "Get Trainee Trainings List by trainee username and criteria". */
    @RequireAuthentication
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional(readOnly = true)
    public List<Training> getTraineeTrainings(Credentials credentials, String traineeUsername, LocalDate fromDate,
                                               LocalDate toDate, String trainerName, String trainingTypeName) {
        return trainingDao.selectByTraineeUsernameAndCriteria(traineeUsername, fromDate, toDate, trainerName, trainingTypeName);
    }

    /** Point 15: "Get Trainer Trainings List by trainer username and criteria". */
    @RequireAuthentication
    @EnableArgumentLogging
    @EnableReturnValueLogging
    @EnableExecutionTimeLogging
    @Transactional(readOnly = true)
    public List<Training> getTrainerTrainings(Credentials credentials, String trainerUsername, LocalDate fromDate,
                                               LocalDate toDate, String traineeName) {
        return trainingDao.selectByTrainerUsernameAndCriteria(trainerUsername, fromDate, toDate, traineeName);
    }

    private void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
