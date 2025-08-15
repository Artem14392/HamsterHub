package org.example.tracker;

import org.example.tracker.db.HamsterTrackerRepository;
import org.example.tracker.domain.DailyReport;
import org.example.tracker.domain.DefoltReportGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class GeneratorTest {
    @Test
    void reportDate_isToday_andActiveFlag_gt10() {
        HamsterTrackerRepository repo = mock(HamsterTrackerRepository.class);
        when(repo.getAllRoundsSnapshot()).thenReturn(Map.of(
                "h1", 10,   // не активен (нужно >10)
                "h2", 11    // активен
        ));

        DefoltReportGenerator gen = new DefoltReportGenerator(repo);
        DailyReport r = gen.generateDailyReport();

        assertThat(r.getDate()).isEqualTo(LocalDate.now());
        assertThat(r.getHamsterStats().get("h1").isActive()).isFalse();
        assertThat(r.getHamsterStats().get("h2").isActive()).isTrue();
    }

    @Test
    void stats_sortedByHamsterId_lexicographically() {
        HamsterTrackerRepository repo = mock(HamsterTrackerRepository.class);
        when(repo.getAllRoundsSnapshot()).thenReturn(Map.of(
                "h2", 5,
                "h10", 6,
                "h1", 7
        ));

        DefoltReportGenerator gen = new DefoltReportGenerator(repo);
        DailyReport r = gen.generateDailyReport();

        assertThat(new ArrayList<>(r.getHamsterStats().keySet()))
                .containsExactly("h1", "h10", "h2");
    }
}
