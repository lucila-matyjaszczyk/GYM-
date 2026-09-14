package com.gymcrm.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A gym trainee profile.
 * <p>
 * This used to {@code extend User} ("a Trainee IS A User"). Now, matching
 * the DB schema -- where {@code Trainee} has its OWN {@code ID (PK)} plus a
 * separate {@code UserId (FK)} column -- it HAS a {@link User} instead,
 * through a {@code @OneToOne} association. Practical consequence: {@code
 * getId()} (this trainee's own primary key) and {@code getUser().getUserId()}
 * (the linked user row's primary key) are now two different numbers, where
 * before there was only ever one ("the" id, inherited from User).
 */
@Entity
@Table(name = "trainee")
public class Trainee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address")
    private String address;

    // cascade = ALL + orphanRemoval = true: a Trainee "owns" its User the
    // way a folder owns the one document inside it -- saving/updating/
    // deleting the Trainee automatically saves/updates/deletes its User
    // too, so callers never have to remember to persist both separately.
    // (@OneToOne defaults to eager loading, which is what we want here:
    // we need the username/password practically every time we load a
    // trainee, e.g. to authenticate it.)
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // The owning side of the many-to-many with Trainer: @JoinTable tells
    // Hibernate to create a third, "junction" table (trainee_trainer) with
    // just two FK columns, one row per (trainee, trainer) pairing -- that's
    // how a many-to-many is actually represented in a relational database,
    // there's no such thing as a column that holds "many" foreign keys.
    @ManyToMany
    @JoinTable(
            name = "trainee_trainer",
            joinColumns = @JoinColumn(name = "trainee_id"),
            inverseJoinColumns = @JoinColumn(name = "trainer_id")
    )
    private Set<Trainer> trainers = new HashSet<>();

    public Trainee() {
    }

    public Trainee(String firstName, String lastName) {
        this.user = new User(firstName, lastName);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Set<Trainer> getTrainers() {
        return trainers;
    }

    public void setTrainers(Set<Trainer> trainers) {
        this.trainers = trainers;
    }

    // --- Convenience delegates to the linked User ---
    // Purely so the Service/Dao code we already wrote (which today calls
    // things like trainee.getUsername() directly, back from when Trainee
    // extended User) keeps reading naturally as "trainee.getUsername()"
    // instead of "trainee.getUser().getUsername()" everywhere. We'll still
    // have to go through that calling code by hand in the next steps and
    // swap every getUserId() for getId() -- that one's NOT delegated on
    // purpose, since "the trainee's id" now means something different than
    // "the linked user's id", and papering over that with a delegate would
    // silently produce the wrong number.
    public String getFirstName() {
        return user.getFirstName();
    }

    public void setFirstName(String firstName) {
        user.setFirstName(firstName);
    }

    public String getLastName() {
        return user.getLastName();
    }

    public void setLastName(String lastName) {
        user.setLastName(lastName);
    }

    public String getUsername() {
        return user.getUsername();
    }

    public void setUsername(String username) {
        user.setUsername(username);
    }

    public String getPassword() {
        return user.getPassword();
    }

    public void setPassword(String password) {
        user.setPassword(password);
    }

    public boolean isActive() {
        return user.isActive();
    }

    public void setActive(boolean active) {
        user.setActive(active);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trainee)) return false;
        Trainee trainee = (Trainee) o;
        return Objects.equals(id, trainee.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Deliberately does NOT print "trainers": Trainer.toString() would, in
    // turn, print its own "trainees" set -- which would print their
    // "trainers" again, forever, until the program crashes with a
    // StackOverflowError. Whenever two entities reference each other back
    // and forth like this, at least one side has to stay out of toString
    // (and out of equals/hashCode, for the same reason).
    @Override
    public String toString() {
        return "Trainee{" +
                "id=" + id +
                ", user=" + user +
                ", dateOfBirth=" + dateOfBirth +
                ", address='" + address + '\'' +
                '}';
    }
}
