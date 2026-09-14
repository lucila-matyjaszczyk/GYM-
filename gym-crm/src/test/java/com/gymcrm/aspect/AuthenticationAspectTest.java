package com.gymcrm.aspect;

import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.security.AuthenticationException;
import com.gymcrm.security.Credentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AuthenticationAspect}'s own logic
 * ({@code checkAuthentication}), calling it directly rather than going
 * through a real Spring AOP proxy -- that keeps this test fast and focused
 * on "does the credential-matching decision come out right", the same way
 * every other unit test in this project isolates one class with mocks.
 * <p>
 * Whether Spring actually WIRES this aspect onto every
 * {@code @RequireAuthentication} method, and whether the {@code @Order}/
 * {@code @EnableTransactionManagement(order = 0)} ordering fix really works
 * end to end, is a different question -- that's what {@code
 * AppConfigIntegrationTest}'s {@code requireAuthentication_realProxy_*}
 * tests check, using the real Spring context.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationAspectTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    @InjectMocks
    private AuthenticationAspect authenticationAspect;

    @Test
    void checkAuthentication_matchingTraineeCredentials_doesNotThrow() {
        when(traineeDao.matchesCredentials("John.Smith", "pass1")).thenReturn(true);

        assertDoesNotThrow(() -> authenticationAspect.checkAuthentication(new Credentials("John.Smith", "pass1")));
        verify(trainerDao, never()).matchesCredentials(anyString(), anyString());
    }

    @Test
    void checkAuthentication_matchingTrainerCredentials_doesNotThrow() {
        when(traineeDao.matchesCredentials("Mike.Johnson", "pass1")).thenReturn(false);
        when(trainerDao.matchesCredentials("Mike.Johnson", "pass1")).thenReturn(true);

        assertDoesNotThrow(() -> authenticationAspect.checkAuthentication(new Credentials("Mike.Johnson", "pass1")));
    }

    @Test
    void checkAuthentication_matchingNeither_throwsAuthenticationException() {
        when(traineeDao.matchesCredentials("Nobody", "wrong")).thenReturn(false);
        when(trainerDao.matchesCredentials("Nobody", "wrong")).thenReturn(false);

        assertThrows(AuthenticationException.class,
                () -> authenticationAspect.checkAuthentication(new Credentials("Nobody", "wrong")));
    }

    @Test
    void checkAuthentication_nullCredentials_throwsAuthenticationExceptionWithoutTouchingDaos() {
        assertThrows(AuthenticationException.class, () -> authenticationAspect.checkAuthentication(null));

        verify(traineeDao, never()).matchesCredentials(anyString(), anyString());
        verify(trainerDao, never()).matchesCredentials(anyString(), anyString());
    }
}
