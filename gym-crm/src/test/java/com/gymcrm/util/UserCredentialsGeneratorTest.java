package com.gymcrm.util;

import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.TrainingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCredentialsGeneratorTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    @InjectMocks
    private UserCredentialsGenerator generator;

    @Test
    void generateUsername_noCollision_returnsBaseUsername() {
        when(traineeDao.selectAll()).thenReturn(List.of());
        when(trainerDao.selectAll()).thenReturn(List.of());

        String username = generator.generateUsername("John", "Smith");

        assertEquals("John.Smith", username);
    }

    @Test
    void generateUsername_collisionWithAnotherTrainee_appendsSerial1() {
        Trainee existing = new Trainee("John", "Smith");
        existing.setUsername("John.Smith");
        when(traineeDao.selectAll()).thenReturn(List.of(existing));
        when(trainerDao.selectAll()).thenReturn(List.of());

        String username = generator.generateUsername("John", "Smith");

        assertEquals("John.Smith1", username);
    }

    @Test
    void generateUsername_collisionWithATrainer_appendsSerial1() {
        // this is exactly the bug we fixed: a Trainer with the same username
        // must block a Trainee from getting it too, and vice versa
        Trainer existingTrainer = new Trainer("John", "Smith", new TrainingType("Fitness"));
        existingTrainer.setUsername("John.Smith");
        when(traineeDao.selectAll()).thenReturn(List.of());
        when(trainerDao.selectAll()).thenReturn(List.of(existingTrainer));

        String username = generator.generateUsername("John", "Smith");

        assertEquals("John.Smith1", username);
    }

    @Test
    void generateUsername_multipleCollisionsAcrossBothTypes_appendsNextFreeSerial() {
        Trainee traineeA = new Trainee("John", "Smith");
        traineeA.setUsername("John.Smith");
        Trainee traineeB = new Trainee("John", "Smith");
        traineeB.setUsername("John.Smith1");
        Trainer trainer = new Trainer("John", "Smith", new TrainingType("Fitness"));
        trainer.setUsername("John.Smith2");

        when(traineeDao.selectAll()).thenReturn(List.of(traineeA, traineeB));
        when(trainerDao.selectAll()).thenReturn(List.of(trainer));

        String username = generator.generateUsername("John", "Smith");

        assertEquals("John.Smith3", username);
    }

    @Test
    void generatePassword_hasExpectedLength() {
        String password = generator.generatePassword();

        assertEquals(10, password.length());
    }

    @Test
    void generatePassword_onlyContainsLettersAndDigits() {
        String password = generator.generatePassword();

        assertTrue(password.chars().allMatch(Character::isLetterOrDigit));
    }

    @Test
    void generatePassword_generatesDifferentValuesOnEachCall() {
        String first = generator.generatePassword();
        String second = generator.generatePassword();

        assertNotEquals(first, second);
    }
}
