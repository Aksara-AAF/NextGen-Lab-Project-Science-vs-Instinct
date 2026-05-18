package com.nextgenlab.backend.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "achievements")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    public Achievement() {}

    public Achievement(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public Long getId()              { return id; }
    public String getCode()          { return code; }
    public void setCode(String v)    { this.code = v; }
    public String getName()          { return name; }
    public void setName(String v)    { this.name = v; }
    public String getDescription()   { return description; }
    public void setDescription(String v) { this.description = v; }
}
