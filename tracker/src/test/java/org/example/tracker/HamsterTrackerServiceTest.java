package org.example.tracker;
import org.example.tracker.db.InMemoryHamsterTrackerRepository;
import org.example.tracker.domain.AlertService;
import org.example.tracker.domain.DailyReport;
import org.example.tracker.domain.HamsterTrackerService;
import org.example.tracker.domain.ReportGenerator;
import org.example.tracker.domain.eventDto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class HamsterTrackerServiceTest {

    InMemoryHamsterTrackerRepository repo;
    AlertService alerts;
    ReportGenerator reports;
    HamsterTrackerService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryHamsterTrackerRepository();
        alerts = mock(AlertService.class);
        reports = mock(ReportGenerator.class);
        service = new HamsterTrackerService(repo, alerts, reports);
    }

    @Test
    void wheelSpin_addsOnlyFullRounds_whenHamsterInWheel() {
        service.accept(new HamsterEnter("h1", "w1"));
        service.accept(new WheelSpin("w1", 10_500)); // 2 полных круга
        assertThat(repo.getRounds("h1")).isEqualTo(2);
    }

    @Test
    void wheelSpin_negativeIgnored_shortUpdatesActivityOnly() {
        service.accept(new HamsterEnter("h1", "w1"));

        service.accept(new WheelSpin("w1", -1000));
        assertThat(repo.getRounds("h1")).isZero();

        Instant before = repo.getAllLastActivity().get("h1");
        service.accept(new WheelSpin("w1", 4_999));
        Instant after = repo.getAllLastActivity().get("h1");
        assertThat(repo.getRounds("h1")).isZero();
        assertThat(after).isAfterOrEqualTo(before);
    }

    @Test
    void duplicateEnter_ignored_butTouchesActivity() {
        service.accept(new HamsterEnter("h1", "w1"));
        Instant before = repo.getAllLastActivity().get("h1");

        service.accept(new HamsterEnter("h1", "w1")); // дубль

        assertThat(repo.getOccupant("w1")).isEqualTo("h1");
        Instant after = repo.getAllLastActivity().get("h1");
        assertThat(after).isAfterOrEqualTo(before);
    }

    @Test
    void exitOfAnotherHamster_ignored_butTouchesThatHamster() {
        service.accept(new HamsterEnter("hA", "w1"));
        service.accept(new HamsterExit("hB", "w1"));

        assertThat(repo.getOccupant("w1")).isEqualTo("hA");
        assertThat(repo.getAllLastActivity().get("hB")).isNotNull();
    }

    @Test
    void inactivityAlert_sent_whenMoreThan1Hour() throws Exception {
        repo.updateLastActivity("h1", Instant.now().minus(Duration.ofHours(2)));
        repo.updateLastActivity("h2", Instant.now().minus(Duration.ofMinutes(30)));

        Method m = HamsterTrackerService.class.getDeclaredMethod("checkInactivity");
        m.setAccessible(true);
        m.invoke(service);

        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(alerts, times(1)).sendAlert(msg.capture());
        assertThat(msg.getValue()).contains("Hamster h1 inactive");
        assertThat(repo.isInactivityAlerted("h1")).isTrue();
    }

    @Test
    void sensorDownAlert_sent_whenMoreThan30Minutes() throws Exception {
        repo.markSensorFailed("s1", Instant.now().minus(Duration.ofMinutes(31)));
        repo.markSensorFailed("s2", Instant.now().minus(Duration.ofMinutes(5)));

        Method m = HamsterTrackerService.class.getDeclaredMethod("checkSensorsDown");
        m.setAccessible(true);
        m.invoke(service);

        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(alerts, times(1)).sendAlert(msg.capture());
        assertThat(msg.getValue()).contains("Sensor s1 is down");
        assertThat(repo.isSensorAlerted("s1")).isTrue();
    }

    @Test
    void generateDailyReport_resetsRounds() {
        DailyReport stub = new DailyReport(java.time.LocalDate.now(), java.util.Map.of());
        when(reports.generateDailyReport()).thenReturn(stub);

        service.accept(new HamsterEnter("h1", "w1"));
        service.accept(new WheelSpin("w1", 15_000)); // 3 круга
        assertThat(repo.getRounds("h1")).isEqualTo(3);

        DailyReport out = service.generateDailyReport();
        assertThat(out).isSameAs(stub);
        assertThat(repo.getRounds("h1")).isZero();
    }
}
