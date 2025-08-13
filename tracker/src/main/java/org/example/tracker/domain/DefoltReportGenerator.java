package org.example.tracker.domain;

import org.example.tracker.db.HamsterTrackerRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DefoltReportGenerator implements   ReportGenerator{
    private static final int ACTIVE_THRESHOLD = 10;
    private final HamsterTrackerRepository repository;

    public DefoltReportGenerator(HamsterTrackerRepository repository) {
        this.repository = repository;
    }

    @Override
    public DailyReport generateDailyReport() {
        LocalDate today = LocalDate.now();
        Map<String, Integer> roundsByHamster = repository.getAllRoundsSnapshot();

        Map<String, HamsterStats> hamsterStatsMap = new LinkedHashMap<>();
        roundsByHamster.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String hamsterId = entry.getKey();
                    int totalRounds = entry.getValue();
                    boolean active = totalRounds > ACTIVE_THRESHOLD;
                    hamsterStatsMap.put(hamsterId, new HamsterStats(hamsterId, totalRounds, active));
                });

        return new DailyReport(today, hamsterStatsMap);
    }
}
