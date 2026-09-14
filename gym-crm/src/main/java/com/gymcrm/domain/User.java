package com.gymcrm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * A person who can log into the gym CRM -- login/profile fields shared by
 * trainees and trainers.
 * <p>
 * Until now this used to be the common superclass of {@link Trainee} and
 * {@link Trainer} ("a Trainee IS A User"). To match the DB schema exactly
 * (where Trainee/Trainer each have their own PK plus a separate {@code
 * UserId} foreign key column), it's now its own standalone table that a
 * Trainee/Trainer HAS one of, connected by a {@code @OneToOne} association
 * instead of Java inheritance -- see the updated {@link Trainee}/{@link
 * Trainer} classes for that side of the relationship.
 * <p>
 * Table named {@code users} (plural) instead of {@code user}: "USER" is a
 * reserved SQL keyword in H2 (and most other databases), so a table
 * literally named {@code user} would either fail to create or force us to
 * quote it everywhere.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long userId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // Hibernate needs a no-arg constructor (at least protected) to build a
    // "blank" instance before filling in its fields from a query result --
    // public here because Service code will also call it directly now to
    // build a brand-new User before attaching it to a Trainee/Trainer.
    public User() {
    }

    public User(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", username='" + username + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
