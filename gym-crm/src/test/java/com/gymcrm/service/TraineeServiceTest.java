package com.gymcrm.service;

import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.security.Credentials;
import com.gymcrm.util.UserCredentialsGenerator;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Note: these are Mockito unit tests, so {@code TraineeService} is
 * instantiated directly, without going through a Spring AOP proxy -- that
 * means {@code @RequireAuthentication} is never actually enforced here (the
 * real {@code Credentials} we pass in are never even validated). That's
 * intentional and matches how every other unit test in this project works:
 * this class only tests TraineeService's OWN logic in isolation. The
 * authentication check itself is tested on its own in
 * {@code AuthenticationAspectTest}, and the full, real, wired-together
 * behavior is tested in {@code AppConfigIntegrationTest}.
 */
@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private UserCredentialsGenerator credentialsGenerator;

    @InjectMocks
    private TraineeService traineeService;

    private static final Credentials CREDENTIALS = new Credentials("John.Smith", "secretPass1");

    @Test
    void createTraineeProfile_generatesCredentialsAndDelegatesCreationToDao() {
        when(credentialsGenerator.generateUsername("John", "Smith")).thenReturn("John.Smith");
        when(credentialsGenerator.generatePassword()).thenReturn("aB3dE9fG1k");
        when(traineeDao.create(any(Trainee.class))).thenAnswer(invocation -> {
            Trainee saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        Trainee result = traineeService.createTraineeProfile(
                "John", "Smith", LocalDate.of(1995, 3, 14), "123 Main St");

        assertEquals("John.Smith", result.getUsername());
        assertEquals("aB3dE9fG1k", result.getPassword());
        assertTrue(result.isActive());
        assertEquals(99L, result.getId());
        verify(credentialsGenerator).generateUsername("John", "Smith");
    }

    @Test
    void createTraineeProfile_blankFirstName_throwsAndNeverTouchesDao() {
        assertThrows(IllegalArgumentException.class,
                () -> traineeService.createTraineeProfile(" ", "Smith", null, "123 Main St"));

        verify(traineeDao, never()).create(any());
    }

    @Test
    void authenticate_delegatesToDao() {
        when(traineeDao.matchesCredentials("John.Smith", "secretPass1")).thenReturn(true);

        assertTrue(traineeService.authenticate(CREDENTIALS));
    }

    @Test
    void updateTraineeProfile_delegatesToDao() {
        Trainee trainee = new Trainee("John", "Smith");
        trainee.setId(1L);
        when(traineeDao.update(trainee)).thenReturn(trainee);

        Trainee result = traineeService.updateTraineeProfile(CREDENTIALS, trainee);

        assertSame(trainee, result);
        verify(traineeDao).update(trainee);
    }

    @Test
    void updateTraineeProfile_blankLastName_throwsAndNeverTouchesDao() {
        Trainee trainee = new Trainee("John", "Smith");
        trainee.setLastName(" ");

        assertThrows(IllegalArgumentException.class, () -> traineeService.updateTraineeProfile(CREDENTIALS, trainee));
        verify(traineeDao, never()).update(any());
    }

    @Test
    void deleteTraineeProfile_delegatesToDao() {
        traineeService.deleteTraineeProfile(CREDENTIALS, 5L);

        verify(traineeDao).delete(5L);
    }

    @Test
    void selectTraineeProfile_delegatesToDao() {
        Trainee trainee = new Trainee("John", "Smith");
        when(traineeDao.select(2L)).thenReturn(trainee);

        Trainee result = traineeService.selectTraineeProfile(CREDENTIALS, 2L);

        assertSame(trainee, result);
    }

    @Test
    void selectTraineeProfileByUsername_delegatesToDao() {
        Trainee trainee = new Trainee("John", "Smith");
        when(traineeDao.selectByUsername("John.Smith")).thenReturn(trainee);

        Trainee result = traineeService.selectTraineeProfileByUsername(CREDENTIALS, "John.Smith");

        assertSame(trainee, result);
    }

    @Test
    void changeTraineePassword_delegatesToDao() {
        traineeService.changeTraineePassword(CREDENTIALS, 1L, "newPassword1");

        verify(traineeDao).changePassword(1L, "newPassword1");
    }

    @Test
    void changeTraineePassword_blankNewPassword_throwsAndNeverTouchesDao() {
        assertThrows(IllegalArgumentException.class,
                () -> traineeService.changeTraineePassword(CREDENTIALS, 1L, " "));

        verify(traineeDao, never()).changePassword(any(), any());
    }

    @Test
    void activateTrainee_delegatesToDao() {
        traineeService.activateTrainee(CREDENTIALS, 1L);

        verify(traineeDao).activate(1L);
    }

    @Test
    void deactivateTrainee_delegatesToDao() {
        traineeService.deactivateTrainee(CREDENTIALS, 1L);

        verify(traineeDao).deactivate(1L);
    }

    @Test
    void getTrainersNotAssigned_delegatesToTrainerDao() {
        Trainer trainer = new Trainer("Mike", "Johnson", new TrainingType("Fitness"));
        when(trainerDao.selectNotAssignedToTraineeUsername("John.Smith")).thenReturn(List.of(trainer));

        List<Trainer> result = traineeService.getTrainersNotAssigned(CREDENTIALS, "John.Smith");

        assertEquals(1, result.size());
        assertSame(trainer, result.get(0));
    }

    @Test
    void updateTraineeTrainers_resolvesEachUsernameAndDelegatesToDao() {
        Trainer trainerA = new Trainer("Mike", "Johnson", new TrainingType("Fitness"));
        Trainer trainerB = new Trainer("Anna", "Lee", new TrainingType("Yoga"));
        when(trainerDao.selectByUsername("Mike.Johnson")).thenReturn(trainerA);
        when(trainerDao.selectByUsername("Anna.Lee")).thenReturn(trainerB);
        Trainee trainee = new Trainee("John", "Smith");
        when(traineeDao.updateTrainersList(eq(1L), any())).thenReturn(trainee);

        Trainee result = traineeService.updateTraineeTrainers(
                CREDENTIALS, 1L, List.of("Mike.Johnson", "Anna.Lee"));

        assertSame(trainee, result);
    }

    @Test
    void updateTraineeTrainers_unknownUsername_throwsEntityNotFoundExceptionAndNeverTouchesDao() {
        when(trainerDao.selectByUsername("Ghost")).thenReturn(null);

        assertThrows(EntityNotFoundException.class,
                () -> traineeService.updateTraineeTrainers(CREDENTIALS, 1L, List.of("Ghost")));

        verify(traineeDao, never()).updateTrainersList(any(), any());
    }
}
