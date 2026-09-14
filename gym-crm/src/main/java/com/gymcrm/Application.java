package com.gymcrm;

import com.gymcrm.config.AppConfig;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.facade.GymFacade;
import com.gymcrm.security.Credentials;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

/**
 * Small manual entry point, not required by the task itself, but handy to
 * actually SEE the whole thing work end to end against the real H2 database:
 * it boots the Spring context from {@link AppConfig}, creates a brand-new
 * trainee and trainer (so you can see the generated username/password), and
 * then re-reads each one back -- this time going through the authenticated
 * path, using the very credentials we just generated, exactly like note 2
 * of the task requires for anything that isn't a profile creation.
 * <p>
 * Run this class directly (IDE "Run" button, or {@code mvn exec:java}) once
 * dependencies are downloaded, and check the console/logs.
 */
public class Application {

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {

            GymFacade gymFacade = context.getBean(GymFacade.class);

            // 1. Create a trainee. Points 1 and 2 of the task are the only
            //    operations that need no credentials at all -- there's
            //    nothing to authenticate against yet.
            Trainee newTrainee = gymFacade.createTraineeProfile(
                    "Maria", "Gonzalez", null, "Av. Siempre Viva 742");
            System.out.println("New trainee created -> username: " + newTrainee.getUsername()
                    + ", password: " + newTrainee.getPassword());
            Credentials traineeCredentials = new Credentials(newTrainee.getUsername(), newTrainee.getPassword());

            // 2. Trainer.specialization is a real foreign key now, so we
            //    need an actual, already-persisted TrainingType (one of the
            //    five TrainingTypeSeeder created at startup) before we can
            //    create a trainer.
            List<TrainingType> specializations = gymFacade.getAvailableSpecializations();
            TrainingType zumba = specializations.stream()
                    .filter(type -> "Zumba".equals(type.getTrainingTypeName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Zumba training type was not seeded"));

            // 3. Create a trainer -- also no credentials needed yet.
            Trainer newTrainer = gymFacade.createTrainerProfile("Carlos", "Ruiz", zumba);
            System.out.println("New trainer created -> username: " + newTrainer.getUsername()
                    + ", password: " + newTrainer.getPassword());
            Credentials trainerCredentials = new Credentials(newTrainer.getUsername(), newTrainer.getPassword());

            // 4. From here on every call needs valid credentials as its
            //    first argument -- AuthenticationAspect checks them before
            //    the method body even runs.
            Trainee reloadedTrainee = gymFacade.selectTraineeProfile(traineeCredentials, newTrainee.getId());
            System.out.println("Reloaded trainee (authenticated) -> " + reloadedTrainee);

            Trainer reloadedTrainer = gymFacade.selectTrainerProfile(trainerCredentials, newTrainer.getId());
            System.out.println("Reloaded trainer (authenticated) -> " + reloadedTrainer);
        }
    }
}
