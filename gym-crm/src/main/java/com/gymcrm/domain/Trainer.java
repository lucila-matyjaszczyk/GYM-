package com.gymcrm.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A gym trainer profile. Same composition change as {@link Trainee}: HAS a
 * {@link User} (via {@code @OneToOne}) instead of extending it.
 */
@Entity
@Table(name = "trainer")
public class Trainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Same reasoning as Trainee.user: the Trainer owns its User row.
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Many trainers can share the same specialization (e.g. several Yoga
    // trainers), so this is @ManyToOne, not @OneToOne -- it just points at
    // one row of the small, fixed TrainingType lookup table.
    @ManyToOne
    @JoinColumn(name = "specialization_id")
    private TrainingType specialization;

    // The INVERSE side of the many-to-many from Trainee: mappedBy tells
    // Hibernate "don't create your own join table for this side, the
    // trainee_trainer table declared over on Trainee.trainers already
    // covers this relationship -- just read it the other way around".
    // Exactly one side of a @ManyToMany owns the @JoinTable; the other
    // side always uses mappedBy pointing at the owning side's field name.
    @ManyToMany(mappedBy = "trainers")
    private Set<Trainee> trainees = new HashSet<>();

    public Trainer() {
    }

    public Trainer(String firstName, String lastName, TrainingType specialization) {
        this.user = new User(firstName, lastName);
        this.specialization = specialization;
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

    public TrainingType getSpecialization() {
        return specialization;
    }

    public void setSpecialization(TrainingType specialization) {
        this.specialization = specialization;
    }

    public Set<Trainee> getTrainees() {
        return trainees;
    }

    public void setTrainees(Set<Trainee> trainees) {
        this.trainees = trainees;
    }

    // --- Convenience delegates to the linked User (see Trainee for why) ---
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
        if (!(o instanceof Trainer)) return false;
        Trainer trainer = (Trainer) o;
        return Objects.equals(id, trainer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Same rule as Trainee: never print/compare "trainees" here, or a
    // Trainer<->Trainee print would recurse forever.
    @Override
    public String toString() {
        return "Trainer{" +
                "id=" + id +
                ", user=" + user +
                ", specialization=" + specialization +
                '}';
    }
}
