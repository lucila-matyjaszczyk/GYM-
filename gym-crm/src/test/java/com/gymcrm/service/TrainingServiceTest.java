package com.gymcrm.service;

import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.dao.TrainingDao;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.security.Credentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock
    private TrainingDao trainingDao;

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    @InjectMocks
    private TrainingService trainingService;

    private static final Credentials CREDENTIALS = new Credentials("John.Smith", "secretPass1");
    private static final TrainingType FITNESS = new TrainingType("Fitness");

    @Test
    void createTraining_whenTraineeAndTrainerExist_delegatesToDao() {
        Trainee trainee = new Trainee("John", "Smith");
        Trainer trainer = new Trainer("Mike", "Johnson", FITNESS);
        when(traineeDao.selectByUsername("John.Smith")).thenReturn(trainee);
        when(trainerDao.selectByUsername("Mike.Johnson")).thenReturn(trainer);
        when(trainingDao.create(org.mockito.ArgumentMatchers.any(Training.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Training result = trainingService.createTraining(CREDENTIALS, "John.Smith", "Mike.Johnson",
                "Morning Fitness", FITNESS, LocalDate.of(2026, 6, 10), 60);

        assertEquals("Morning Fitness", result.getTrainingName());
        assertSame(trainee, result.getTrainee());
        assertSame(trainer, result.getTrainer());
        verify(trainingDao).create(result);
    }

    @Test
    void createTraining_whenTraineeDoesNotExist_throwsAndNeverCallsDao() {
        when(traineeDao.selectByUsername("John.Smith")).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> trainingService.createTraining(
                CREDENTIALS, "John.Smith", "Mike.Johnson", "Morning Fitness",
                FITNESS, LocalDate.of(2026, 6, 10), 60));

        verify(trainingDao, never()).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createTraining_whenTrainerDoesNotExist_throwsAndNeverCallsDao() {
        when(traineeDao.selectByUsername("John.Smith")).thenReturn(new Trainee("John", "Smith"));
        when(trainerDao.selectByUsername("Mike.Johnson")).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> trainingService.createTraining(
                CREDENTIALS, "John.Smith", "Mike.Johnson", "Morning Fitness",
                FITNESS, LocalDate.of(2026, 6, 10), 60));

        verify(trainingDao, never()).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createTraining_blankTrainingName_throwsAndNeverResolvesTraineeOrTrainer() {
        assertThrows(IllegalArgumentException.class, () -> trainingService.createTraining(
                CREDENTIALS, "John.Smith", "Mike.Johnson", " ",
                FITNESS, LocalDate.of(2026, 6, 10), 60));

        verify(traineeDao, never()).selectByUsername(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void selectTraining_delegatesToDao() {
        Training training = new Training(new Trainee("John", "Smith"), new Trainer("Mike", "Johnson", FITNESS),
                "Morning Fitness", FITNESS, LocalDate.of(2026, 6, 10), 60);
        when(trainingDao.select(7L)).thenReturn(training);

        Training result = trainingService.selectTraining(CREDENTIALS, 7L);

        assertSame(training, result);
    }

    @Test
    void getTraineeTrainings_delegatesToDaoWithAllCriteria() {
        Training training = new Training(new Trainee("John", "Smith"), new Trainer("Mike", "Johnson", FITNESS),
                "Morning Fitness", FITNESS, LocalDate.of(2026, 6, 10), 60);
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        when(trainingDao.selectByTraineeUsernameAndCriteria("John.Smith", from, to, "Johnson", "Fitness"))
                .thenReturn(List.of(training));

        List<Training> result = trainingService.getTraineeTrainings(
                CREDENTIALS, "John.Smith", from, to, "Johnson", "Fitness");

        assertEquals(1, result.size());
        assertSame(training, result.get(0));
    }

    @Test
    void getTrainerTrainings_delegatesToDaoWithAllCriteria() {
        Training training = new Training(new Trainee("John", "Smith"), new Trainer("Mike", "Johnson", FITNESS),
                "Morning Fitness", FITNESS, LocalDate.of(2026, 6, 10), 60);
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        when(trainingDao.selectByTrainerUsernameAndCriteria("Mike.Johnson", from, to, "Smith"))
                .thenReturn(List.of(training));

        List<Training> result = trainingService.getTrainerTrainings(
                CREDENTIALS, "Mike.Johnson", from, to, "Smith");

        assertEquals(1, result.size());
        assertSame(training, result.get(0));
    }
}
