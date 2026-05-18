package com.nextgenlab.backend.seeder;

import com.nextgenlab.backend.model.entity.Achievement;
import com.nextgenlab.backend.model.entity.EvolutionGene;
import com.nextgenlab.backend.repository.AchievementRepository;
import com.nextgenlab.backend.repository.EvolutionGeneRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder {

    private final EvolutionGeneRepository geneRepo;
    private final AchievementRepository   achievementRepo;

    public DataSeeder(EvolutionGeneRepository geneRepo, AchievementRepository achievementRepo) {
        this.geneRepo        = geneRepo;
        this.achievementRepo = achievementRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        seedAchievements();
        if (geneRepo.count() == 0) {
            List<EvolutionGene> genes = Arrays.asList(
                gene("PREDATOR_CLAWS", "Predator Claws", "+1 melee damage per hit",
                     "{\"type\":\"melee_damage\",\"value\":1}"),
                gene("ADRENAL_SURGE",  "Adrenal Surge",  "+20% movement speed",
                     "{\"type\":\"speed_mult\",\"value\":1.2}"),
                gene("THICK_HIDE",     "Thick Hide",     "+1 max HP",
                     "{\"type\":\"max_hp\",\"value\":1}"),
                gene("FRENZY",         "Frenzy",         "Dash cooldown -1s",
                     "{\"type\":\"dash_cooldown\",\"value\":-1}"),
                gene("ECHOLOCATION",   "Echolocation",   "Highlight Researcher position for 3s (30s cooldown)",
                     "{\"type\":\"reveal\",\"duration\":3,\"cooldown\":30}"),
                gene("ACIDIC_BLOOD",   "Acidic Blood",   "Fires projectile toward attacker when hit",
                     "{\"type\":\"reactive_projectile\"}"),
                gene("REGENERATION",   "Regeneration",   "+0.5 HP/sec when not in combat",
                     "{\"type\":\"regen\",\"value\":0.5}"),
                gene("TOXIC_AURA",     "Toxic Aura",     "Slows Researcher when in 2-tile radius",
                     "{\"type\":\"slow_aura\",\"radius\":64}"),
                gene("PHASE_SHIFT",    "Phase Shift",    "Walk through walls once (20s cooldown)",
                     "{\"type\":\"wall_pass\",\"cooldown\":20}"),
                gene("BERSERKER",      "Berserker",      "+30% damage when HP < 2",
                     "{\"type\":\"berserker\",\"threshold\":2,\"mult\":1.3}")
            );
            geneRepo.saveAll(genes);
            System.out.println("[DataSeeder] Seeded " + genes.size() + " evolution genes.");
        }
    }

    private void seedAchievements() {
        if (achievementRepo.count() > 0) return;
        List<Achievement> achievements = Arrays.asList(
            new Achievement("FIRST_WIN",      "First Win",      "Menangkan pertandingan pertamamu"),
            new Achievement("VETERAN",         "Veteran",         "Mainkan 10 pertandingan"),
            new Achievement("MONSTER_HUNTER",  "Monster Hunter",  "Menangkan 5x sebagai Peneliti"),
            new Achievement("LAB_DEFENDER",    "Lab Defender",    "Menangkan 5x sebagai Monster"),
            new Achievement("SPEED_DEMON",     "Speed Demon",     "Menangkan dalam waktu < 3 menit"),
            new Achievement("SURVIVOR",        "Survivor",        "Menangkan 10 pertandingan"),
            new Achievement("SHARPSHOOTER",    "Sharpshooter",    "Mainkan 20x sebagai Peneliti"),
            new Achievement("APEX_PREDATOR",   "Apex Predator",   "Mainkan 20x sebagai Monster"),
            new Achievement("ELO_MASTER",      "ELO Master",      "Capai ELO 1200"),
            new Achievement("LEGEND",          "Legend",          "Capai ELO 1500")
        );
        achievementRepo.saveAll(achievements);
        System.out.println("[DataSeeder] Seeded " + achievements.size() + " achievements.");
    }

    private EvolutionGene gene(String code, String name, String desc, String effectJson) {
        EvolutionGene g = new EvolutionGene();
        g.setCode(code);
        g.setName(name);
        g.setDescription(desc);
        g.setEffectJson(effectJson);
        return g;
    }
}
