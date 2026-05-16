package com.nextgenlab.backend.model.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long    getId()           { return id; }
    public String  getEmail()        { return email; }
    public String  getUsername()     { return username; }
    public String  getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt()    { return createdAt; }

    public void setEmail(String email)               { this.email = email; }
    public void setUsername(String username)         { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
