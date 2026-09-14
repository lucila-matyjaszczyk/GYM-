package com.gymcrm.service;

import com.gymcrm.dao.TrainerDao;
import com.gymcrm.dao.TrainingTypeDao;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.security.Credentials;
import com.gymcrm.util.UserCredentialsGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mockito unit tests for {@link TrainerService} -- see the class comment on
 * {@code TraineeServiceTest} for why {@code @RequireAuthentication} is not,
 * and does not need to be, exercised here.
 */
@ExtendWith(MockitoExtension.class)
class TrainerServiceTest {

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private TrainingTypeDao trainingTypeDao;

    @Mock
    private UserCredentialsGenerator credentialsGenerator;

    @InjectMocks
    private TrainerService trainerService;

    private static final Credentials CREDENTIALS = new Credentials("Mike.Johnson", "secretPass1");

    @Test
    void createTrainerProfile_generatesCredentialsAndDelegatesCreationToDao() {
        when(credentialsGenerator.generateUsername("Mike", "Johnson")).thenReturn("Mike.Johnson");
        when(credentialsGenerator.generatePassword()).thenReturn("zR5vN8cX2q");
        when(trainerDao.create(any(Trainer.class))).thenAnswer(invocation -> {
            Trainer saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TrainingType specialization = new TrainingType("Fitness");
        Trainer result = trainerService.createTrainerProfile("Mike", "Johnson", specialization);

        assertEquals("Mike.Johnson", result.getUsername());
        assertEquals("zR5vN8cX2q", result.getPassword());
        assertTrue(result.isActive());
        assertEquals(specialization, result.getSpecialization());
        assertEquals(1L, result.getId());
        verify(credentialsGenerator).generateUsername("Mike", "Johnson");
    }

    @Test
    void createTrainerProfile_nullSpecialization_throwsAndNeverTouchesDao() {
        assertThrows(IllegalArgumentException.class,
                () -> trainerService.createTrainerProfile("Mike", "Johnson", null));

        verify(trainerDao, never()).create(any());
    }

    @Test
    void getAvailableSpecializations_delegatesToTrainingTypeDao() {
        List<TrainingType> types = List.of(new TrainingType("Fitness"), new TrainingType("Yoga"));
        when(trainingTypeDao.selectAll()).thenReturn(types);

        List<TrainingType> result = trainerService.getAvailableSpecializations();

        assertSame(types, result);
    }

    @Test
    void authenticate_delegatesToDao() {
        when(trainerDao.matchesCredentials("Mike.Johnson", "secretPass1")).thenReturn(true);

        assertTrue(trainerService.authenticate(CREDENTIALS));
    }

    @Test
    void updateTrainerProfile_delegatesToDao() {
        Trainer trainer = new Trainer("Mike", "Johnson", new TrainingType("Fitness"));
        when(trainerDao.update(trainer)).thenReturn(trainer);

        Trainer result = trainerService.updateTrainerProfile(CREDENTIALS, trainer);

        assertSame(trainer, result);
    }

    @Test
    void selectTrainerProfile_delegatesToDao() {
        Trainer trainer = new Trainer("Mike", "Johnson", new TrainingType("Fitness"));
        when(trainerDao.select(3L)).thenReturn(trainer);

        Trainer result = trainerService.selectTrainerProfile(CREDENTIALS, 3L);

        assertSame(trainer, result);
    }

    @Test
    void selectTrainerProfileByUsername_delegatesToDao() {
        Trainer trainer = new Trainer("Mike", "Johnson", new TrainingType("Fitness"));
        when(trainerDao.selectByUsername("Mike.Johnson")).thenReturn(trainer);

        Trainer result = trainerService.selectTrainerProfileByUsername(CREDENTIALS, "Mike.Johnson");

        assertSame(trainer, result);
    }

    @Test
    void changeTrainerPassword_delegatesToDao() {
        trainerService.changeTrainerPassword(CREDENTIALS, 1L, "newPassword1");

        verify(trainerDao).changePassword(1L, "newPassword1");
    }

    @Test
    void changeTrainerPassword_blankNewPassword_throwsAndNeverTouchesDao() {
        assertThrows(IllegalArgumentException.class,
                () -> trainerService.changeTrainerPassword(CREDENTIALS, 1L, " "));

        verify(trainerDao, never()).changePassword(any(), any());
    }

    @Test
    void activateTrainer_delegatesToDao() {
        trainerService.activateTrainer(CREDENTIALS, 1L);

        verify(trainerDao).activate(1L);
    }

    @Test
    void deactivateTrainer_delegatesToDao() {
        trainerService.deactivateTrainer(CREDENTIALS, 1L);

        verify(trainerDao).deactivate(1L);
    }
}
