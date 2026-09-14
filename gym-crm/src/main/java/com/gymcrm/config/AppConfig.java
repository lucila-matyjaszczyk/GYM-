package com.gymcrm.config;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Java-based Spring configuration.
 * <ul>
 *   <li>{@code @ComponentScan} tells Spring to look through every class under
 *   {@code com.gymcrm} and turn the ones annotated with {@code @Component},
 *   {@code @Repository}, {@code @Service}, etc. into beans.</li>
 *   <li>{@code @PropertySource} loads {@code application.properties} (the
 *   "external property file" the task mentions) so that {@code @Value("${...}")}
 *   placeholders can be resolved, e.g. the DB connection settings below.</li>
 *   <li>{@code @EnableAspectJAutoProxy} turns on Spring AOP: without it,
 *   {@code LoggingAspect} would just sit there as an unused bean -- Spring
 *   would never wrap the annotated Service methods with a proxy that
 *   actually applies it.</li>
 *   <li>{@code @EnableTransactionManagement} turns on {@code @Transactional}:
 *   without it, that annotation would just be inert text sitting on top of
 *   a method, because nothing would be scanning for it and wrapping the
 *   method in a transaction proxy. {@code order = 0} makes the transaction
 *   proxy wrap AROUND our other aspects (like {@code AuthenticationAspect}),
 *   so a transaction (and its Hibernate Session) is already open by the
 *   time those aspects run -- see {@code AuthenticationAspect} for why that
 *   ordering matters.</li>
 * </ul>
 *
 * <p>Note: despite the package name {@code org.springframework.orm.hibernate5},
 * this is the correct, current integration Spring Framework 6.x ships for
 * Hibernate ORM -- Spring never renamed the package when Hibernate moved to
 * version 6, since the {@code SessionFactory}/{@code Session} API it wraps
 * did not change in a way that required a new one.</p>
 */
@Configuration
@ComponentScan(basePackages = "com.gymcrm")
@PropertySource("classpath:application.properties")
@EnableAspectJAutoProxy
@EnableTransactionManagement(order = 0)
public class AppConfig {

    // Must be a *static* method: this particular bean has to be ready and
    // working before any other bean in the context gets created, otherwise
    // no ${...} placeholder anywhere would be resolvable yet.
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    /**
     * The "phone line" to the H2 database: knows the driver class, the JDBC
     * URL, and the credentials, but nothing yet about Hibernate or our
     * entities. {@code DriverManagerDataSource} opens a brand-new physical
     * connection every time one is requested -- perfectly fine for a
     * learning project; a real production app would normally swap this for
     * a pooled DataSource (e.g. HikariCP) that reuses connections.
     */
    @Bean
    public DataSource dataSource(@Value("${db.driver}") String driverClassName,
                                  @Value("${db.url}") String url,
                                  @Value("${db.username}") String username,
                                  @Value("${db.password}") String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    /**
     * Builds Hibernate's {@link SessionFactory} as a Spring bean. This is
     * the "manager" Hibernate builds exactly once at startup, after reading
     * every {@code @Entity} class under {@code com.gymcrm.domain} (that's
     * what {@code packagesToScan} does) and working out how each one maps
     * to a table.
     */
    @Bean
    public LocalSessionFactoryBean sessionFactory(DataSource dataSource) {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setPackagesToScan("com.gymcrm.domain");

        Properties hibernateProperties = new Properties();
        hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        // "update": at startup, Hibernate compares our @Entity classes
        // against the tables that already exist in the DB file and creates/
        // adjusts whatever is missing, without dropping existing data. Handy
        // while we're still building the schema; a real production app
        // would normally manage schema changes with a migration tool
        // instead (e.g. Flyway/Liquibase) and set this to "validate".
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "update");
        // Purely for learning purposes: prints every SQL statement Hibernate
        // actually runs to the console, nicely indented, so we can see the
        // translation from our Java calls into real SQL as we go.
        hibernateProperties.setProperty("hibernate.show_sql", "true");
        hibernateProperties.setProperty("hibernate.format_sql", "true");
        sessionFactory.setHibernateProperties(hibernateProperties);

        return sessionFactory;
    }

    /**
     * Lets {@code @Transactional} work against a Hibernate {@code Session}
     * (instead of raw JDBC): opens a transaction before an annotated method
     * runs, commits it if the method finishes normally, and rolls it back
     * if the method throws.
     */
    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }
}
