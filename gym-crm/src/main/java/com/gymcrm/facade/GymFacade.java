package com.gymcrm.facade;

import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.security.Credentials;
import com.gymcrm.service.TraineeService;
import com.gymcrm.service.TrainerService;
import com.gymcrm.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Single entry point to the whole Spring Core + Hibernate module: everything
 * a caller (a test, a future REST controller, {@code Application}'s main
 * method) needs is exposed here, so nobody outside this package has to know
 * that TraineeService, TrainerService and TrainingService even exist as
 * separate classes.
 * <p>
 * Every method here just forwards, unchanged, to the matching Service
 * method -- including the {@code Credentials} parameter that most of them
 * now require (see note 2 of the Hibernate task, and {@code
 * AuthenticationAspect}). This class adds no logic of its own on purpose:
 * it's a thin façade, not a fourth layer of business rules.
 */
@Component
public class GymFacade {

    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;

    // Constructor-based injection, exactly as the task asks for
    // "services beans should be injected into the facade".
    @Autowired
    public GymFacade(TraineeService traineeService, TrainerService trainerService, TrainingService trainingService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
    }

    // ---- Trainee ----

    /** Point 1: "Create Trainee profile". No credentials needed -- there's nothing to authenticate yet. */
    public Trainee createTraineeProfile(String firstName, String lastName, LocalDate dateOfBirth, String address) {
        return traineeService.createTraineeProfile(firstName, lastName, dateOfBirth, address);
    }

    /** Point 3: "Trainee username and password matching". */
    public boolean authenticateTrainee(Credentials credentials) {
        return traineeService.authenticate(credentials);
    }

    /** Point 5: "Update Trainee profile". */
    public Trainee updateTraineeProfile(Credentials credentials, Trainee trainee) {
        return traineeService.updateTraineeProfile(credentials, trainee);
    }

    /** Point 9: "Delete a Trainee profile". */
    public void deleteTraineeProfile(Credentials credentials, Long traineeId) {
        traineeService.deleteTraineeProfile(credentials, traineeId);
    }

    /** Point 5 (select side): "Select Trainee profile by ID". */
    public Trainee selectTraineeProfile(Credentials credentials, Long traineeId) {
        return traineeService.selectTraineeProfile(credentials, traineeId);
    }

    /** Point 6: "Select Trainee profile by username". */
    public Trainee selectTraineeProfileByUsername(Credentials credentials, String username) {
        return traineeService.selectTraineeProfileByUsername(credentials, username);
    }

    /** Point 7: "Trainee password change". */
    public void changeTraineePassword(Credentials credentials, Long traineeId, String newPassword) {
        traineeService.changeTraineePassword(credentials, traineeId, newPassword);
    }

    /** Point 11: "Activate/De-activate trainee". */
    public void activateTrainee(Credentials credentials, Long traineeId) {
        traineeService.activateTrainee(credentials, traineeId);
    }

    public void deactivateTrainee(Credentials credentials, Long traineeId) {
        traineeService.deactivateTrainee(credentials, traineeId);
    }

    /** Point 17: "Get trainers list that not assigned on trainee by trainee's username". */
    public List<Trainer> getTrainersNotAssigned(Credentials credentials, String traineeUsername) {
        return traineeService.getTrainersNotAssigned(credentials, traineeUsername);
    }

    /** Point 18: "Update Trainee's trainers list". */
    public Trainee updateTraineeTrainers(Credentials credentials, Long traineeId, List<String> trainerUsernames) {
        return traineeService.updateTraineeTrainers(credentials, traineeId, trainerUsernames);
    }

    // ---- Trainer ----

    /** Point 2: "Create Trainer profile". Same as createTraineeProfile: no credentials needed. */
    public Trainer createTrainerProfile(String firstName, String lastName, TrainingType specialization) {
        return trainerService.createTrainerProfile(firstName, lastName, specialization);
    }

    /** Lists the persisted training types, so a caller has something real to pass as a specialization. */
    public List<TrainingType> getAvailableSpecializations() {
        return trainerService.getAvailableSpecializations();
    }

    /** Point 4: "Trainer username and password matching". */
    public boolean authenticateTrainer(Credentials credentials) {
        return trainerService.authenticate(credentials);
    }

    public Trainer updateTrainerProfile(Credentials credentials, Trainer trainer) {
        return trainerService.updateTrainerProfile(credentials, trainer);
    }

    public Trainer selectTrainerProfile(Credentials credentials, Long trainerId) {
        return trainerService.selectTrainerProfile(credentials, trainerId);
    }

    /** Point 5 (mirrored for trainer): "Select Trainer profile by username". */
    public Trainer selectTrainerProfileByUsername(Credentials credentials, String username) {
        return trainerService.selectTrainerProfileByUsername(credentials, username);
    }

    /** Point 8: "Trainer password change". */
    public void changeTrainerPassword(Credentials credentials, Long trainerId, String newPassword) {
        trainerService.changeTrainerPassword(credentials, trainerId, newPassword);
    }

    /** Point 12: "Activate/De-activate trainer". */
    public void activateTrainer(Credentials credentials, Long trainerId) {
        trainerService.activateTrainer(credentials, trainerId);
    }

    public void deactivateTrainer(Credentials credentials, Long trainerId) {
        trainerService.deactivateTrainer(credentials, trainerId);
    }

    // ---- Training ----

    /** Point 16: "Add training". */
    public Training createTraining(Credentials credentials, String traineeUsername, String trainerUsername,
                                    String trainingName, TrainingType trainingType, LocalDate trainingDate,
                                    Integer trainingDuration) {
        return trainingService.createTraining(credentials, traineeUsername, trainerUsername, trainingName,
                trainingType, trainingDate, trainingDuration);
    }

    public Training selectTraining(Credentials credentials, Long trainingId) {
        return trainingService.selectTraining(credentials, trainingId);
    }

    /** Point 14: "Get Trainee Trainings List by trainee username and criteria". */
    public List<Training> getTraineeTrainings(Credentials credentials, String traineeUsername, LocalDate fromDate,
                                               LocalDate toDate, String trainerName, String trainingTypeName) {
        return trainingService.getTraineeTrainings(credentials, traineeUsername, fromDate, toDate,
                trainerName, trainingTypeName);
    }

    /** Point 15: "Get Trainer Trainings List by trainer username and criteria". */
    public List<Training> getTrainerTrainings(Credentials credentials, String trainerUsername, LocalDate fromDate,
                                               LocalDate toDate, String traineeName) {
        return trainingService.getTrainerTrainings(credentials, trainerUsername, fromDate, toDate, traineeName);
    }
}
