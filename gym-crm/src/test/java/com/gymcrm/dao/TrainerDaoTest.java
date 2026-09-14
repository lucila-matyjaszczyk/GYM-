package com.gymcrm.dao;

import com.gymcrm.config.TestAppConfig;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.exception.InvalidProfileStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real Hibernate integration tests for {@link TrainerDao} -- same idea and
 * same {@link TestAppConfig} as {@code TraineeDaoTest}, see that class'
 * Javadoc for the full explanation of the annotations here.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestAppConfig.class)
@Transactional
class TrainerDaoTest {

    @Autowired
    private TrainerDao trainerDao;

    @Autowired
    private TraineeDao traineeDao;

    @Autowired
    private TrainingTypeDao trainingTypeDao;

    private TrainingType fitness() {
        return trainingTypeDao.selectAll().stream()
                .filter(t -> t.getTrainingTypeName().equals("Fitness"))
                .findFirst().orElseThrow();
    }

    private Trainer newTrainer(String firstName, String lastName) {
        Trainer trainer = new Trainer(firstName, lastName, fitness());
        trainer.setUsername(firstName + "." + lastName);
        trainer.setPassword("secretPass1");
        trainer.setActive(true);
        return trainer;
    }

    @Test
    void create_persistsTrainerAndCascadesIntoItsUser() {
        Trainer created = trainerDao.create(newTrainer("Mike", "Johnson"));

        assertNotNull(created.getId());
        assertNotNull(created.getUser().getUserId());
    }

    @Test
    void update_whenTrainerExists_updatesItsFields() {
        Trainer created = trainerDao.create(newTrainer("Mike", "Johnson"));
        created.setLastName("Johnsonson");

        Trainer updated = trainerDao.update(created);

        assertEquals("Johnsonson", updated.getLastName());
    }

    @Test
    void update_whenTrainerDoesNotExist_throwsEntityNotFoundException() {
        Trainer ghost = newTrainer("No", "Body");
        ghost.setId(999_999L);

        assertThrows(EntityNotFoundException.class, () -> trainerDao.update(ghost));
    }

    @Test
    void selectByUsername_findsTheMatchingTrainer() {
        trainerDao.create(newTrainer("Mike", "Johnson"));

        Trainer found = trainerDao.selectByUsername("Mike.Johnson");

        assertNotNull(found);
        assertEquals("Mike.Johnson", found.getUsername());
    }

    @Test
    void matchesCredentials_correctPassword_returnsTrue_wrongPassword_returnsFalse() {
        trainerDao.create(newTrainer("Mike", "Johnson"));

        assertTrue(trainerDao.matchesCredentials("Mike.Johnson", "secretPass1"));
        assertFalse(trainerDao.matchesCredentials("Mike.Johnson", "wrongPassword"));
    }

    @Test
    void changePassword_updatesThePasswordOnTheLinkedUser() {
        Trainer created = trainerDao.create(newTrainer("Mike", "Johnson"));

        trainerDao.changePassword(created.getId(), "brandNewPass2");

        assertTrue(trainerDao.matchesCredentials("Mike.Johnson", "brandNewPass2"));
    }

    @Test
    void activate_whenAlreadyActive_throwsInvalidProfileStateException() {
        Trainer created = trainerDao.create(newTrainer("Mike", "Johnson")); // active=true already

        assertThrows(InvalidProfileStateException.class, () -> trainerDao.activate(created.getId()));
    }

    @Test
    void deactivate_whenAlreadyInactive_throwsInvalidProfileStateException() {
        Trainer created = trainerDao.create(newTrainer("Mike", "Johnson"));
        trainerDao.deactivate(created.getId());

        assertThrows(InvalidProfileStateException.class, () -> trainerDao.deactivate(created.getId()));
    }

    @Test
    void selectNotAssignedToTraineeUsername_whenNoneAssigned_returnsEveryTrainer() {
        trainerDao.create(newTrainer("Mike", "Johnson"));
        trainerDao.create(newTrainer("Anna", "Lee"));
        Trainee trainee = newUnassignedTrainee();

        List<Trainer> unassigned = trainerDao.selectNotAssignedToTraineeUsername(trainee.getUsername());

        assertEquals(2, unassigned.size());
    }

    @Test
    void selectNotAssignedToTraineeUsername_excludesTrainersAlreadyAssigned() {
        Trainer assigned = trainerDao.create(newTrainer("Mike", "Johnson"));
        Trainer notAssigned = trainerDao.create(newTrainer("Anna", "Lee"));
        Trainee trainee = newUnassignedTrainee();
        trainee.setTrainers(new HashSet<>(Set.of(assigned)));
        traineeDao.update(trainee);

        List<Trainer> result = trainerDao.selectNotAssignedToTraineeUsername(trainee.getUsername());

        assertEquals(1, result.size());
        assertEquals(notAssigned.getId(), result.get(0).getId());
    }

    @Test
    void selectNotAssignedToTraineeUsername_whenTraineeDoesNotExist_throwsEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class,
                () -> trainerDao.selectNotAssignedToTraineeUsername("nobody.here"));
    }

    private Trainee newUnassignedTrainee() {
        Trainee trainee = new Trainee("John", "Smith" + System.nanoTime());
        trainee.setUsername("John.Smith" + System.nanoTime());
        trainee.setPassword("secretPass1");
        trainee.setActive(true);
        return traineeDao.create(trainee);
    }
}
