package org.example.tracker.domain;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.tracker.db.HamsterTrackerRepository;
import org.example.tracker.domain.eventDto.HamsterEvent;
import org.example.tracker.domain.eventDto.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class HamsterTrackerService {

    private final HamsterTrackerRepository hamsterRepository;
    private final AlertService alertService;
    private final ReportGenerator reportGenerator;

    private static final long ROUND_MS = 5_000L;
    private static final int ACTIVE_THRESHOLD = 10;
    private static final Duration INACTIVITY = Duration.ofHours(1);
    private static final Duration SENSOR_DOWN_THRESHOLD = Duration.ofMinutes(30);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "hamster-tracker-scheduler");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> checksTask;
    private ScheduledFuture<?> dailyReportTask;

    public HamsterTrackerService(HamsterTrackerRepository hamsterRepository,
                                 AlertService alertService,
                                 ReportGenerator reportGenerator) {
        this.hamsterRepository = hamsterRepository;
        this.alertService = alertService;
        this.reportGenerator = reportGenerator;
    }

    @PostConstruct
    void start() {
        this.checksTask = scheduler.scheduleAtFixedRate(this::runPeriodicChecksSafe, 60, 60, TimeUnit.SECONDS);
        scheduleDailyReportAtMidnight(ZoneId.systemDefault());
        log.info("HamsterTrackerService started: periodic checks each 60s, daily reports at midnight.");
    }

    public void accept(HamsterEvent event) {
        if (event == null) {
            log.warn("Null event received");
            return;
        }
        try {
            switch (event) {
                case HamsterEnter e -> handleEnter(e);
                case HamsterExit e -> handleExit(e);
                case WheelSpin e -> handleWheelSpin(e);
                case SensorFailure e -> handleSensorFailure(e);
                default -> log.warn("Unknown event type: {}", event.getClass().getName());
            }
        } catch (Exception ex) {
            log.error("Failed to process event {}: {}", event, ex.toString(), ex);
        }
    }

    private void handleEnter(HamsterEnter e) {
        String current = hamsterRepository.getOccupant(e.getWheelId());
        if (Objects.equals(current, e.getHamsterId())) {
            log.debug("Duplicate enter ignored: hamster={} wheel={}", e.getHamsterId(), e.getWheelId());
            touch(e.getHamsterId());
            return;
        }
        hamsterRepository.setOccupant(e.getWheelId(), e.getHamsterId());
        touch(e.getHamsterId());
        log.debug("Enter: hamster={} wheel={}", e.getHamsterId(), e.getWheelId());
    }

    private void handleExit(HamsterExit e) {
        String current = hamsterRepository.getOccupant(e.getWheelId());
        if (!Objects.equals(current, e.getHamsterId())) {
            log.debug("Exit mismatch ignored: hamster={} wheel={} (current={})",
                    e.getHamsterId(), e.getWheelId(), current);
            touch(e.getHamsterId());
            return;
        }
        hamsterRepository.clearOccupantIfMatches(e.getWheelId(), e.getHamsterId());
        touch(e.getHamsterId());
        log.debug("Exit: hamster={} wheel={}", e.getHamsterId(), e.getWheelId());
    }

    private void handleWheelSpin(WheelSpin e) {
        long ms = e.getDurationMs();
        if (ms <= 0) {
            log.debug("Non-positive spin ignored: wheel={} durationMs={}", e.getWheelId(), ms);
            return;
        }
        String hamsterId = hamsterRepository.getOccupant(e.getWheelId());
        if (hamsterId == null) {
            log.debug("Spin ignored: no occupant for wheel={} (durationMs={})", e.getWheelId(), ms);
            return;
        }
        int rounds = (int) (ms / ROUND_MS);
        if (rounds <= 0) {
            touch(hamsterId);
            log.debug("Short spin (<1 round) credited as activity: hamster={} ms={}", hamsterId, ms);
            return;
        }
        int total = hamsterRepository.addRounds(hamsterId, rounds);
        touch(hamsterId);
        log.debug("Spin credited: hamster={} +{} rounds (total={})", hamsterId, rounds, total);
    }

    private void handleSensorFailure(SensorFailure e) {
        String sensorId = e.getSensorId();
        boolean firstTime = hamsterRepository.markSensorFailed(sensorId, Instant.now());
        if (firstTime) {
            hamsterRepository.setSensorAlerted(sensorId, false);
            log.warn("Sensor {} failed (code={}), failure start recorded", sensorId, e.getErrorCode());
        } else {
            log.debug("Sensor {} failure repeated (code={})", sensorId, e.getErrorCode());
        }
    }

    private void touch(String hamsterId) {
        if (hamsterId == null) return;
        hamsterRepository.updateLastActivity(hamsterId, Instant.now());
        hamsterRepository.setInactivityAlerted(hamsterId, false);
    }

    private void runPeriodicChecksSafe() {
        try {
            checkInactivity();
        } catch (Exception ex) {
            log.error("Inactivity check failed: {}", ex.toString(), ex);
        }
        try {
            checkSensorsDown();
        } catch (Exception ex) {
            log.error("Sensor down check failed: {}", ex.toString(), ex);
        }
    }

    private void checkInactivity() {
        final Instant now = Instant.now();
        hamsterRepository.getAllLastActivity().forEach((hamsterId, last) -> {
            if (hamsterId == null || last == null) return;
            Duration idle = Duration.between(last, now);
            if (idle.compareTo(INACTIVITY) > 0 && !hamsterRepository.isInactivityAlerted(hamsterId)) {
                sendAlertSafe("Hamster %s inactive for %d minutes".formatted(hamsterId, idle.toMinutes()));
                hamsterRepository.setInactivityAlerted(hamsterId, true);
            }
        });
    }

    private void checkSensorsDown() {
        final Instant now = Instant.now();
        hamsterRepository.getAllSensorFailures().forEach((sensorId, since) -> {
            if (sensorId == null || since == null) return;
            Duration down = Duration.between(since, now);
            if (down.compareTo(SENSOR_DOWN_THRESHOLD) > 0 && !hamsterRepository.isSensorAlerted(sensorId)) {
                sendAlertSafe("Sensor %s is down for %d minutes".formatted(sensorId, down.toMinutes()));
                hamsterRepository.setSensorAlerted(sensorId, true);
            }
        });
    }

    private void scheduleDailyReportAtMidnight(ZoneId zoneId) {
        Runnable task = this::generateDailyReportSafe;
        long initialDelaySec = secondsUntilNextMidnight(zoneId);
        this.dailyReportTask = scheduler.scheduleAtFixedRate(task, initialDelaySec, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
        log.info("Daily report scheduled: zone={} starts in {} seconds", zoneId, initialDelaySec);
    }

    private long secondsUntilNextMidnight(ZoneId zoneId) {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime nextMidnight = now.truncatedTo(ChronoUnit.DAYS).plusDays(1);
        return Duration.between(now, nextMidnight).getSeconds();
    }

    private void generateDailyReportSafe() {
        try {
            generateDailyReport();
        } catch (Exception ex) {
            log.error("Daily report generation failed: {}", ex.toString(), ex);
        }
    }

    public DailyReport generateDailyReport() {
        DailyReport report = reportGenerator.generateDailyReport();
        hamsterRepository.resetDailyRounds();
        log.info("Daily report generated for date={} hamsters={}", report.getDate(), report.getHamsterStats().size());
        return report;
    }

    private void sendAlertSafe(String msg) {
        try {
            alertService.sendAlert(msg);
        } catch (Exception ex) {
            log.error("Alert send failed: {}", ex.toString(), ex);
        }
    }

}
