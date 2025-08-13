package org.example.tracker.domain;

import lombok.Getter;

import java.time.LocalDate;
import java.util.Map;

@Getter
public class DailyReport {
    private final LocalDate date;
    private final Map<String, HamsterStats> hamsterStats;

    public DailyReport(LocalDate date, Map<String, HamsterStats> hamsterStats) {
        this.date = date;
        this.hamsterStats = hamsterStats;
    }
}
