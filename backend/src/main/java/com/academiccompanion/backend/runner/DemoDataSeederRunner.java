package com.academiccompanion.backend.runner;

import com.academiccompanion.backend.service.DemoDataSeederService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true", matchIfMissing = false)
public class DemoDataSeederRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeederRunner.class);

    private final DemoDataSeederService seederService;

    public DemoDataSeederRunner(DemoDataSeederService seederService) {
        this.seederService = seederService;
    }

    @Override
    public void run(String... args) {
        log.warn("=== DEMO DATA SEED RUNNER EXECUTING: app.seed.demo-data=true was explicitly supplied ===");
        DemoDataSeederService.SeedSummary summary = seederService.executeSeed();
        log.info("Demo data seeding completed with {} collections updated.", summary.getInsertedCounts().size());
        summary.getInsertedCounts().forEach((coll, count) ->
                log.info(" - {}: {} documents inserted", coll, count)
        );
    }
}
