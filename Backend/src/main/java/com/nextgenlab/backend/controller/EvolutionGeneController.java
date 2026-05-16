package com.nextgenlab.backend.controller;

import com.nextgenlab.backend.model.entity.EvolutionGene;
import com.nextgenlab.backend.service.EvolutionLibraryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EvolutionGeneController {

    private final EvolutionLibraryService service;

    public EvolutionGeneController(EvolutionLibraryService service) {
        this.service = service;
    }

    @GetMapping("/evolution-genes")
    public List<EvolutionGene> getAll() {
        return service.findAll();
    }
}
