package com.gymcrm.config;

import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.UUID;

/**
 * A second, TEST-ONLY copy of {@link AppConfig}, used by every DAO
 * integration test (see {@code TraineeDaoTest}, {@code TrainerDaoTest},
 * {@code TrainingDaoTest}, {@code TrainingTypeDaoTest}) and by {@code
 * AuthenticationAspectTest} where relevant. It re-declares the exact same
 * three beans (dataSource / sessionFactory / transactionManager) as {@link
 * AppConfig}, and keeps the same {@code @ComponentScan}/{@code
 * @EnableAspectJAutoProxy}/{@code @EnableTransactionManagement(order = 0)}
 * setup -- so DAO tests run against the same wiring the real app uses -- but
 * points at a throwaway, in-memory H2 database instead of the real
 * {@code ./h2-data/gymcrm} file, so tests never touch (or get confused by)
 * whatever data you've built up running {@code Application} by hand.
 * <p>
 * {@code jdbc:h2:mem:...}: lives purely in RAM, never written to disk.
 * {@code DB_CLOSE_DELAY=-1}: normally an in-memory H2 database is destroyed
 * the instant its one and only connection closes -- since Spring/Hibernate
 * open and close connections constantly (one per transaction), that would
 * wipe the database between almost every single call. This flag tells H2
 * "keep this database alive until the JVM itself shuts down" instead. A
 * random {@link UUID} in the URL keeps this database name unique, so two
 * different test runs (or, in theory, two different Spring contexts built
 * from this same class) never accidentally share -- or collide over -- the
 * same in-memory schema.
 * <p>
 * {@code hibernate.hbm2ddl.auto=create-drop} (instead of {@code AppConfig}'s
 * {@code "update"}): every time this context starts, Hibernate creates every
 * table fresh from our {@code @Entity} classes, and drops them all again
 * when the context closes. Combined with each test method running inside
 * its own transaction that gets rolled back at the end (see {@code
 * @Transactional} on the DAO test classes), this means every single test
 * starts from a guaranteed-empty, known schema -- no leftover data from a
 * previous test run can ever sneak in and make a test pass or fail for the
 * wrong reason.
 */
@Configuration
@ComponentScan(basePackages = "com.gymcrm")
@EnableAspectJAutoProxy
@EnableTransactionManagement(order = 0)
public class TestAppConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:gymcrm_test_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(DataSource dataSource) {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setPackagesToScan("com.gymcrm.domain");

        Properties hibernateProperties = new Properties();
        hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        hibernateProperties.setProperty("hibernate.show_sql", "false");
        sessionFactory.setHibernateProperties(hibernateProperties);

        return sessionFactory;
    }

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }
}
