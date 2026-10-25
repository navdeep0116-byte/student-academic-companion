package com.academiccompanion.backend.runner;

import com.academiccompanion.backend.service.DemoDataSeederService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.seed.level3-targeted", havingValue = "true", matchIfMissing = false)
public class Level3TargetedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Level3TargetedRunner.class);

    private final DemoDataSeederService seederService;

    public Level3TargetedRunner(DemoDataSeederService seederService) {
        this.seederService = seederService;
    }

    @Override
    public void run(String... args) {
        log.warn("=== TARGETED LEVEL 3 RUNNER EXECUTING: app.seed.level3-targeted=true ===");
        Map<String, Object> result = seederService.seedQuiz3ForDhruvKapoor();
        log.info("Targeted Level 3 update result: {}", result);
    }
}
