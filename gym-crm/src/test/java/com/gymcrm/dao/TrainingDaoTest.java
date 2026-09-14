package com.gymcrm.dao;

import com.gymcrm.config.TestAppConfig;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real Hibernate integration tests for {@link TrainingDao} -- same idea and
 * same {@link TestAppConfig} as {@code TraineeDaoTest}. This class is also
 * what exercises the dynamic-HQL criteria building in {@code
 * selectByTraineeUsernameAndCriteria}/{@code selectByTrainerUsernameAndCriteria}
 * (points 14/15 of the task) -- something a mock-based test never could,
 * since the whole point of those methods is the actual SQL that ends up
 * running against real rows.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestAppConfig.class)
@Transactional
class TrainingDaoTest {

    @Autowired
    private TrainingDao trainingDao;

    @Autowired
    private TraineeDao traineeDao;

    @Autowired
    private TrainerDao trainerDao;

    @Autowired
    private TrainingTypeDao trainingTypeDao;

    private Trainee trainee;
    private Trainer trainer;
    private TrainingType fitness;
    private TrainingType yoga;

    @BeforeEach
    void setUp() {
        List<TrainingType> types = trainingTypeDao.selectAll();
        fitness = types.stream().filter(t -> t.getTrainingTypeName().equals("Fitness")).findFirst().orElseThrow();
        yoga = types.stream().filter(t -> t.getTrainingTypeName().equals("Yoga")).findFirst().orElseThrow();

        Trainee newTrainee = new Trainee("John", "Smith");
        newTrainee.setUsername("John.Smith");
        newTrainee.setPassword("secretPass1");
        newTrainee.setActive(true);
        trainee = traineeDao.create(newTrainee);

        Trainer newTrainer = new Trainer("Mike", "Johnson", fitness);
        newTrainer.setUsername("Mike.Johnson");
        newTrainer.setPassword("secretPass1");
        newTrainer.setActive(true);
        trainer = trainerDao.create(newTrainer);
    }

    private Training training(String name, TrainingType type, LocalDate date) {
        return trainingDao.create(new Training(trainee, trainer, name, type, date, 60));
    }

    @Test
    void create_persistsTraining() {
        Training created = training("Morning Fitness", fitness, LocalDate.of(2026, 6, 10));

        assertNotNull(created.getId());
    }

    @Test
    void select_returnsThePersistedTraining() {
        Training created = training("Morning Fitness", fitness, LocalDate.of(2026, 6, 10));

        Training found = trainingDao.select(created.getId());

        assertEquals(created.getId(), found.getId());
    }

    @Test
    void selectByTraineeUsernameAndCriteria_filtersByDateRange() {
        training("January session", fitness, LocalDate.of(2026, 1, 10));
        training("June session", fitness, LocalDate.of(2026, 6, 10));
        training("December session", fitness, LocalDate.of(2026, 12, 10));

        List<Training> result = trainingDao.selectByTraineeUsernameAndCriteria(
                "John.Smith", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 7, 1), null, null);

        assertEquals(1, result.size());
        assertEquals("June session", result.get(0).getTrainingName());
    }

    @Test
    void selectByTraineeUsernameAndCriteria_filtersByTrainingTypeName() {
        training("Fitness session", fitness, LocalDate.of(2026, 6, 10));
        training("Yoga session", yoga, LocalDate.of(2026, 6, 11));

        List<Training> result = trainingDao.selectByTraineeUsernameAndCriteria(
                "John.Smith", null, null, null, "Yoga");

        assertEquals(1, result.size());
        assertEquals("Yoga session", result.get(0).getTrainingName());
    }

    @Test
    void selectByTraineeUsernameAndCriteria_filtersByTrainerNameCaseInsensitively() {
        training("Fitness session", fitness, LocalDate.of(2026, 6, 10));

        List<Training> result = trainingDao.selectByTraineeUsernameAndCriteria(
                "John.Smith", null, null, "johnson", null);

        assertEquals(1, result.size());
    }

    @Test
    void selectByTrainerUsernameAndCriteria_filtersByTraineeName() {
        training("Fitness session", fitness, LocalDate.of(2026, 6, 10));

        List<Training> matching = trainingDao.selectByTrainerUsernameAndCriteria(
                "Mike.Johnson", null, null, "smith");
        List<Training> nonMatching = trainingDao.selectByTrainerUsernameAndCriteria(
                "Mike.Johnson", null, null, "nobody");

        assertEquals(1, matching.size());
        assertTrue(nonMatching.isEmpty());
    }
}
