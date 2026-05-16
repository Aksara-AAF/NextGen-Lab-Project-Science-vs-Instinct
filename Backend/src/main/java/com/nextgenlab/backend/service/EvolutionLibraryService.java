package com.nextgenlab.backend.service;

import com.nextgenlab.backend.model.entity.EvolutionGene;
import com.nextgenlab.backend.repository.EvolutionGeneRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvolutionLibraryService {

    private final EvolutionGeneRepository repo;

    public EvolutionLibraryService(EvolutionGeneRepository repo) {
        this.repo = repo;
    }

    public List<EvolutionGene> findAll() {
        return repo.findAll();
    }
}
