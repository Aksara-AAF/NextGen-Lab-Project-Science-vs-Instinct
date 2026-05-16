package com.nextgenlab.backend.repository;

import com.nextgenlab.backend.model.entity.EvolutionGene;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvolutionGeneRepository extends JpaRepository<EvolutionGene, Long> {
    boolean existsByCode(String code);
}
