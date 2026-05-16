package com.nextgenlab.backend.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "evolution_library")
public class EvolutionGene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "effect_json")
    private String effectJson;

    public EvolutionGene() {}

    public Long   getId()          { return id; }
    public String getCode()        { return code; }
    public String getName()        { return name; }
    public String getDescription() { return description; }
    public String getEffectJson()  { return effectJson; }

    public void setId(Long id)                   { this.id = id; }
    public void setCode(String code)             { this.code = code; }
    public void setName(String name)             { this.name = name; }
    public void setDescription(String desc)      { this.description = desc; }
    public void setEffectJson(String effectJson) { this.effectJson = effectJson; }
}
