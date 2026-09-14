package com.gymcrm.aspect;

import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.security.AuthenticationException;
import com.gymcrm.security.Credentials;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Enforces note 2 of the Hibernate task ("all functions except Create
 * Trainer/Trainee profile should be executed only after Trainee/Trainer
 * authentication") as one cross-cutting concern, instead of repeating a
 * username/password check by hand at the top of every Service method --
 * same idea as {@link LoggingAspect}, just checking something before the
 * method runs instead of logging around it.
 * <p>
 * We don't know, just from a {@code Credentials} value, whether it belongs
 * to a Trainee or a Trainer -- so this checks both DAOs and accepts either
 * match.
 * <p>
 * {@code @Order(1)}: makes this aspect run AFTER the {@code @Transactional}
 * proxy has already opened a Hibernate session for the current thread (see
 * {@code AppConfig}, where {@code @EnableTransactionManagement(order = 0)}
 * gives the transaction proxy a lower order number, which in Spring AOP
 * means higher precedence -- it wraps AROUND this aspect). That ordering
 * matters because {@code matchesCredentials()} below queries the database
 * through {@code sessionFactory.getCurrentSession()}: if this aspect ran
 * BEFORE the transaction opened, there would be no session yet to use, and
 * it would fail with "no session currently bound".
 */
@Aspect
@Component
@Order(1)
public class AuthenticationAspect {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationAspect.class);

    private TraineeDao traineeDao;
    private TrainerDao trainerDao;

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    // args(credentials,..): tells AspectJ "bind whatever the FIRST argument
    // of the intercepted method is into this 'credentials' parameter" (the
    // ",.." means "then ignore however many other arguments follow it").
    // This only matches methods whose first parameter is a Credentials --
    // exactly the convention every @RequireAuthentication method follows.
    @Before("@annotation(com.gymcrm.aspect.annotation.RequireAuthentication) && args(credentials,..)")
    public void checkAuthentication(Credentials credentials) {
        if (credentials == null) {
            throw new AuthenticationException("Missing credentials");
        }

        boolean matchesTrainee = traineeDao.matchesCredentials(credentials.username(), credentials.password());
        boolean matchesTrainer = !matchesTrainee
                && trainerDao.matchesCredentials(credentials.username(), credentials.password());

        if (!matchesTrainee && !matchesTrainer) {
            log.warn("Authentication failed for username={}", credentials.username());
            throw new AuthenticationException(
                    "Invalid username or password for username=" + credentials.username());
        }
        log.debug("Authenticated username={} as {}", credentials.username(),
                matchesTrainee ? "trainee" : "trainer");
    }
}
