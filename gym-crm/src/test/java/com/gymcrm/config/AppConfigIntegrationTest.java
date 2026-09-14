package com.gymcrm.config;

import com.gymcrm.aspect.AuthenticationAspect;
import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.dao.TrainingDao;
import com.gymcrm.dao.TrainingTypeDao;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.facade.GymFacade;
import com.gymcrm.security.AuthenticationException;
import com.gymcrm.security.Credentials;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unlike every other test class, this one is a true end-to-end integration
 * test: it boots the REAL Spring context from {@link AppConfig} (the exact
 * configuration {@code Application} uses), against the real H2 database file
 * under {@code ./h2-data/gymcrm}. A unit test with mocks can never exercise
 * AppConfig's actual wiring -- the {@code @PropertySource} reading
 * {@code application.properties}, the real {@code DataSource}/{@code
 * SessionFactory}/{@code HibernateTransactionManager} beans, or (most
 * importantly) whether Spring actually applies {@code AuthenticationAspect}
 * to every {@code @RequireAuthentication} method through a real AOP proxy,
 * in the right order relative to {@code @Transactional} -- only actually
 * starting the context and calling through it can prove that.
 * <p>
 * This is safe to run repeatedly: {@code TrainingTypeSeeder} only inserts
 * its fixed catalog once (it checks the count first), and every trainee
 * created here uses a unique, timestamp-based username so re-running this
 * test never collides with data left over from a previous run.
 */
class AppConfigIntegrationTest {

    private static AnnotationConfigApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @Test
    void contextContainsAllExpectedBeans() {
        assertNotNull(context.getBean(GymFacade.class));
        assertNotNull(context.getBean(TraineeDao.class));
        assertNotNull(context.getBean(TrainerDao.class));
        assertNotNull(context.getBean(TrainingDao.class));
        assertNotNull(context.getBean(TrainingTypeDao.class));
        assertNotNull(context.getBean(AuthenticationAspect.class));
    }

    @Test
    void trainingTypeSeeder_seededTheFixedCatalogOfTrainingTypes() {
        GymFacade gymFacade = context.getBean(GymFacade.class);

        List<String> names = gymFacade.getAvailableSpecializations().stream()
                .map(TrainingType::getTrainingTypeName)
                .toList();

        assertTrue(names.containsAll(List.of("Fitness", "Yoga", "Zumba", "Stretching", "Resistance")));
    }

    /**
     * The single most important test in this whole project for THIS module:
     * it's the one that would have caught the {@code @Order}/
     * {@code @EnableTransactionManagement(order = 0)} ordering mistake we
     * carefully worked around in {@code AppConfig} and {@code
     * AuthenticationAspect}. If that ordering were wrong, this call would
     * fail with a Hibernate "no session currently bound" error instead of
     * the {@link AuthenticationException} we actually expect -- proving the
     * transaction really is open before the authentication check runs.
     */
    @Test
    void requireAuthentication_realProxy_rejectsInvalidCredentials() {
        GymFacade gymFacade = context.getBean(GymFacade.class);
        Credentials badCredentials = new Credentials("does.not.exist." + System.nanoTime(), "wrongPassword");

        assertThrows(AuthenticationException.class, () -> gymFacade.selectTraineeProfile(badCredentials, 1L));
    }

    @Test
    void requireAuthentication_realProxy_acceptsValidCredentials() {
        GymFacade gymFacade = context.getBean(GymFacade.class);
        Trainee trainee = gymFacade.createTraineeProfile(
                "Integration", "Test" + System.nanoTime(), LocalDate.of(1990, 1, 1), "Test Address");
        Credentials credentials = new Credentials(trainee.getUsername(), trainee.getPassword());

        Trainee reloaded = gymFacade.selectTraineeProfile(credentials, trainee.getId());

        assertEquals(trainee.getId(), reloaded.getId());
    }
}
