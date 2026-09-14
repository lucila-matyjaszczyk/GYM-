package com.gymcrm.facade;

import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.security.Credentials;
import com.gymcrm.service.TraineeService;
import com.gymcrm.service.TrainerService;
import com.gymcrm.service.TrainingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * These tests only check that GymFacade forwards to the right service with
 * the right arguments — the actual business logic is already covered by
 * each service's own tests.
 */
@ExtendWith(MockitoExtension.class)
class GymFacadeTest {

    @Mock
    private TraineeService traineeService;

    @Mock
    private TrainerService trainerService;

    @Mock
    private TrainingService trainingService;

    @InjectMocks
    private GymFacade gymFacade;

    private static final Credentials CREDENTIALS = new Credentials("John.Smith", "secretPass1");

    @Test
    void createTraineeProfile_delegatesToTraineeService() {
        Trainee trainee = new Trainee("John", "Smith");
        when(traineeService.createTraineeProfile("John", "Smith", null, "123 Main St"))
                .thenReturn(trainee);

        Trainee result = gymFacade.createTraineeProfile("John", "Smith", null, "123 Main St");

        assertSame(trainee, result);
    }

    @Test
    void authenticateTrainee_delegatesToTraineeService() {
        when(traineeService.authenticate(CREDENTIALS)).thenReturn(true);

        assertTrue(gymFacade.authenticateTrainee(CREDENTIALS));
    }

    @Test
    void deleteTraineeProfile_delegatesToTraineeService() {
        gymFacade.deleteTraineeProfile(CREDENTIALS, 5L);

        verify(traineeService).deleteTraineeProfile(CREDENTIALS, 5L);
    }

    @Test
    void updateTraineeTrainers_delegatesToTraineeService() {
        Trainee trainee = new Trainee("John", "Smith");
        when(traineeService.updateTraineeTrainers(CREDENTIALS, 1L, java.util.List.of("Mike.Johnson")))
                .thenReturn(trainee);

        Trainee result = gymFacade.updateTraineeTrainers(CREDENTIALS, 1L, java.util.List.of("Mike.Johnson"));

        assertSame(trainee, result);
    }

    @Test
    void createTrainerProfile_delegatesToTrainerService() {
        TrainingType specialization = new TrainingType("Fitness");
        Trainer trainer = new Trainer("Mike", "Johnson", specialization);
        when(trainerService.createTrainerProfile("Mike", "Johnson", specialization))
                .thenReturn(trainer);

        Trainer result = gymFacade.createTrainerProfile("Mike", "Johnson", specialization);

        assertSame(trainer, result);
    }

    @Test
    void getAvailableSpecializations_delegatesToTrainerService() {
        java.util.List<TrainingType> types = java.util.List.of(new TrainingType("Fitness"));
        when(trainerService.getAvailableSpecializations()).thenReturn(types);

        assertSame(types, gymFacade.getAvailableSpecializations());
    }

    @Test
    void createTraining_delegatesToTrainingService() {
        Trainee trainee = new Trainee("John", "Smith");
        Trainer trainer = new Trainer("Mike", "Johnson", new TrainingType("Fitness"));
        Training training = new Training(trainee, trainer, "Morning Fitness",
                new TrainingType("Fitness"), LocalDate.of(2026, 6, 10), 60);
        when(trainingService.createTraining(CREDENTIALS, "John.Smith", "Mike.Johnson", "Morning Fitness",
                training.getTrainingType(), LocalDate.of(2026, 6, 10), 60)).thenReturn(training);

        Training result = gymFacade.createTraining(CREDENTIALS, "John.Smith", "Mike.Johnson", "Morning Fitness",
                training.getTrainingType(), LocalDate.of(2026, 6, 10), 60);

        assertSame(training, result);
    }

    @Test
    void getTraineeTrainings_delegatesToTrainingService() {
        java.util.List<Training> trainings = java.util.List.of();
        when(trainingService.getTraineeTrainings(CREDENTIALS, "John.Smith", null, null, null, null))
                .thenReturn(trainings);

        assertSame(trainings, gymFacade.getTraineeTrainings(CREDENTIALS, "John.Smith", null, null, null, null));
    }
}
