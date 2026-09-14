package com.gymcrm.util;

import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

/**
 * Calculates the username and password for a brand-new Trainee or Trainer,
 * following the rules from the task:
 * <ul>
 *   <li>username = firstName + "." + lastName (e.g. "John.Smith")</li>
 *   <li>if that username is already taken, append a serial number
 *   ("John.Smith1", "John.Smith2", ...)</li>
 *   <li>password = random 10-character string</li>
 * </ul>
 * The "is it already taken?" check lives here, not in each Service, because
 * usernames have to be unique across BOTH Trainees and Trainers — a
 * Service that only looked at its own DAO could hand out a username that a
 * user of the other type already has. This class asks both {@link TraineeDao}
 * and {@link TrainerDao} for their current usernames every time.
 */
@Component
public class UserCredentialsGenerator {

    private static final Logger log = LoggerFactory.getLogger(UserCredentialsGenerator.class);

    private static final String USERNAME_SEPARATOR = ".";
    private static final int PASSWORD_LENGTH = 10;
    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final SecureRandom random = new SecureRandom();

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

    public String generateUsername(String firstName, String lastName) {
        Set<String> existingUsernames = collectAllExistingUsernames();

        String baseUsername = firstName + USERNAME_SEPARATOR + lastName;
        if (!existingUsernames.contains(baseUsername)) {
            log.debug("Generated username '{}' (no collision)", baseUsername);
            return baseUsername;
        }
        int serial = 1;
        String candidate = baseUsername + serial;
        while (existingUsernames.contains(candidate)) {
            serial++;
            candidate = baseUsername + serial;
        }
        log.debug("Base username '{}' was taken, generated '{}' instead", baseUsername, candidate);
        return candidate;
    }

    public String generatePassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
        }
        // never log the actual password value, even at debug level
        log.debug("Generated a new random password");
        return password.toString();
    }

    private Set<String> collectAllExistingUsernames() {
        Set<String> usernames = new HashSet<>();
        for (Trainee trainee : traineeDao.selectAll()) {
            usernames.add(trainee.getUsername());
        }
        for (Trainer trainer : trainerDao.selectAll()) {
            usernames.add(trainer.getUsername());
        }
        return usernames;
    }
}
