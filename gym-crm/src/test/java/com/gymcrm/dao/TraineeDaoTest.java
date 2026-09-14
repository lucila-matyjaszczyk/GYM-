package com.gymcrm.dao;

import com.gymcrm.config.TestAppConfig;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.exception.InvalidProfileStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real Hibernate integration tests for {@link TraineeDao}: unlike the old
 * (deleted) storage-based version, there's no Storage left to mock -- these
 * tests run the actual HQL/SQL against a real, throwaway H2 database (see
 * {@link TestAppConfig}).
 * <p>
 * {@code @ExtendWith(SpringExtension.class)} + {@code @ContextConfiguration}:
 * tells JUnit "boot a real Spring context from TestAppConfig before running
 * any test in this class, and let me {@code @Autowired} real beans out of
 * it" -- the same idea as {@code @ExtendWith(MockitoExtension.class)} we've
 * used everywhere else, just for a real Spring context instead of Mockito
 * mocks.
 * <p>
 * {@code @Transactional} on the class: Spring's test support wraps EVERY
 * {@code @Test} method in its own transaction, and automatically rolls it
 * back when the method finishes -- pass or fail. Two things fall out of
 * that: (1) it's what gives our DAOs an open Hibernate session to work with
 * ({@code sessionFactory.getCurrentSession()} needs one), exactly like
 * {@code @Transactional} on a Service method does in production; (2)
 * nothing any test method saves ever actually stays in the database, so
 * every test method starts from a clean slate without us having to delete
 * anything by hand.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestAppConfig.class)
@Transactional
class TraineeDaoTest {

    @Autowired
    private TraineeDao traineeDao;

    @Autowired
    private TrainerDao trainerDao;

    @Autowired
    private TrainingDao trainingDao;

    @Autowired
    private TrainingTypeDao trainingTypeDao;

    private Trainee newTrainee(String firstName, String lastName) {
        Trainee trainee = new Trainee(firstName, lastName);
        trainee.setUsername(firstName + "." + lastName);
        trainee.setPassword("secretPass1");
        trainee.setActive(true);
        trainee.setDateOfBirth(LocalDate.of(1995, 3, 14));
        trainee.setAddress("123 Main St");
        return trainee;
    }

    @Test
    void create_persistsTraineeAndCascadesIntoItsUser() {
        Trainee created = traineeDao.create(newTrainee("John", "Smith"));

        assertNotNull(created.getId(), "the trainee itself should have gotten a generated id");
        assertNotNull(created.getUser().getUserId(), "cascade = ALL should have saved the nested User too");
    }

    @Test
    void update_whenTraineeExists_updatesItsFields() {
        Trainee created = traineeDao.create(newTrainee("John", "Smith"));
        created.setAddress("456 Elm St");
        created.setFirstName("Johnny");

        Trainee updated = traineeDao.update(created);

        assertEquals("456 Elm St", updated.getAddress());
        assertEquals("Johnny", updated.getFirstName());
    }

    @Test
    void update_whenTraineeDoesNotExist_throwsEntityNotFoundException() {
        Trainee ghost = newTrainee("No", "Body");
        ghost.setId(999_999L);

        assertThrows(EntityNotFoundException.class, () -> traineeDao.update(ghost));
    }

    @Test
    void delete_removesTraineeAndCascadesIntoItsTrainings() {
        Trainee trainee = traineeDao.create(newTrainee("John", "Smith"));
        Trainer trainer = trainerDao.create(newTrainerFor("Fitness"));
        TrainingType fitness = trainingTypeDao.selectAll().stream()
                .filter(t -> t.getTrainingTypeName().equals("Fitness"))
                .findFirst().orElseThrow();
        Training training = trainingDao.create(new Training(trainee, trainer, "Morning Fitness",
                fitness, LocalDate.of(2026, 6, 10), 60));

        traineeDao.delete(trainee.getId());

        assertNull(traineeDao.select(trainee.getId()), "the trainee itself should be gone");
        assertNull(trainingDao.select(training.getId()), "note 7 of the task: its trainings must be hard-deleted too");
    }

    @Test
    void select_whenNotFound_returnsNull() {
        assertNull(traineeDao.select(999_999L));
    }

    @Test
    void selectByUsername_findsTheMatchingTrainee() {
        traineeDao.create(newTrainee("John", "Smith"));

        Trainee found = traineeDao.selectByUsername("John.Smith");

        assertNotNull(found);
        assertEquals("John.Smith", found.getUsername());
    }

    @Test
    void matchesCredentials_correctPassword_returnsTrue_wrongPassword_returnsFalse() {
        traineeDao.create(newTrainee("John", "Smith"));

        assertTrue(traineeDao.matchesCredentials("John.Smith", "secretPass1"));
        assertFalse(traineeDao.matchesCredentials("John.Smith", "wrongPassword"));
    }

    @Test
    void changePassword_updatesThePasswordOnTheLinkedUser() {
        Trainee created = traineeDao.create(newTrainee("John", "Smith"));

        traineeDao.changePassword(created.getId(), "brandNewPass2");

        assertTrue(traineeDao.matchesCredentials("John.Smith", "brandNewPass2"));
    }

    @Test
    void activate_whenAlreadyActive_throwsInvalidProfileStateException() {
        Trainee created = traineeDao.create(newTrainee("John", "Smith")); // active=true already

        assertThrows(InvalidProfileStateException.class, () -> traineeDao.activate(created.getId()));
    }

    @Test
    void deactivate_thenActivate_flipsTheStateEachTime() {
        Trainee created = traineeDao.create(newTrainee("John", "Smith"));

        traineeDao.deactivate(created.getId());
        assertFalse(traineeDao.select(created.getId()).isActive());

        traineeDao.activate(created.getId());
        assertTrue(traineeDao.select(created.getId()).isActive());
    }

    @Test
    void deactivate_whenAlreadyInactive_throwsInvalidProfileStateException() {
        Trainee created = traineeDao.create(newTrainee("John", "Smith"));
        traineeDao.deactivate(created.getId());

        assertThrows(InvalidProfileStateException.class, () -> traineeDao.deactivate(created.getId()));
    }

    @Test
    void updateTrainersList_replacesTheWholeAssignedSet() {
        Trainee trainee = traineeDao.create(newTrainee("John", "Smith"));
        Trainer trainerA = trainerDao.create(newTrainerFor("Fitness"));
        Trainer trainerB = trainerDao.create(newTrainerFor("Yoga"));

        Set<Trainer> justA = new HashSet<>(List.of(trainerA));
        traineeDao.updateTrainersList(trainee.getId(), justA);
        assertEquals(1, traineeDao.select(trainee.getId()).getTrainers().size());

        Set<Trainer> aAndB = new HashSet<>(List.of(trainerA, trainerB));
        Trainee updated = traineeDao.updateTrainersList(trainee.getId(), aAndB);
        assertEquals(2, updated.getTrainers().size());
    }

    private Trainer newTrainerFor(String trainingTypeName) {
        TrainingType type = trainingTypeDao.selectAll().stream()
                .filter(t -> t.getTrainingTypeName().equals(trainingTypeName))
                .findFirst().orElseThrow();
        Trainer trainer = new Trainer("Mike", "Johnson" + System.nanoTime(), type);
        trainer.setUsername("Mike.Johnson" + System.nanoTime());
        trainer.setPassword("trainerPass1");
        trainer.setActive(true);
        return trainer;
    }
}
