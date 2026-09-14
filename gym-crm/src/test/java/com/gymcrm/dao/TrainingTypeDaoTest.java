package com.gymcrm.dao;

import com.gymcrm.config.TestAppConfig;
import com.gymcrm.domain.TrainingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Also doubles as a check that {@code TrainingTypeSeeder} actually seeded
 * the fixed catalog before any test runs: {@code @PostConstruct} fires once
 * when the {@link TestAppConfig} context starts, well before this test
 * method gets a chance to run.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestAppConfig.class)
@Transactional
class TrainingTypeDaoTest {

    @Autowired
    private TrainingTypeDao trainingTypeDao;

    @Test
    void selectAll_returnsTheFixedSeededCatalog() {
        List<String> names = trainingTypeDao.selectAll().stream()
                .map(TrainingType::getTrainingTypeName)
                .toList();

        assertTrue(names.containsAll(List.of("Fitness", "Yoga", "Zumba", "Stretching", "Resistance")));
    }
}
