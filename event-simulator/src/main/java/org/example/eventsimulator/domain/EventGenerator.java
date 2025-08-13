package org.example.eventsimulator.domain;

import org.example.eventsimulator.eventDto.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class EventGenerator {
    private static final double PER_MIN_ENTER_EXIT = 0.10;  // 10%/мин
    private static final double PER_MIN_FAILURE    = 0.01;  // 1%/мин
    private static final Duration TICK_PERIOD = Duration.ofSeconds(1); // фиксированный тик

    /**
     * Генерирует поток событий для sensorCount датчиков (≤1 событие/сек/датчик).
     * Верхняя граница: sensorCount событий в секунду (например, 10_000 датчиков → 10_000 эвентов/с).
     */
    public Flux<HamsterEvent> stream(int hamsterCount, int sensorCount) {
        if (hamsterCount < 1 || hamsterCount > 10_000) {
            throw new IllegalArgumentException("hamsterCount must be в [1..10000]");
        }
        if (sensorCount < 1 || sensorCount > 10_000) {
            throw new IllegalArgumentException("sensorCount must be в [1..10000]");
        }
        Objects.requireNonNull(TICK_PERIOD, "tickPeriod");

        final double pEnterExit = perTickProbability(PER_MIN_ENTER_EXIT);
        final double pFailure   = perTickProbability(PER_MIN_FAILURE);

        SensorState[] sensors = new SensorState[sensorCount];
        for (int i = 0; i < sensorCount; i++) {
            int hamsterId = (i % hamsterCount) + 1;
            sensors[i] = new SensorState("sensor-" + (i + 1), "wheel-" + (i + 1), "hamster-" + hamsterId);
        }

        // Запускаем генерацию — тик каждые 1 сек
        return Flux.interval(TICK_PERIOD)
                .onBackpressureDrop()
                .flatMap(tick -> Flux
                                .range(0, sensors.length)
                                .mapNotNull(i -> maybeEventForSensor(sensors[i], pEnterExit, pFailure)),
                        Runtime.getRuntime().availableProcessors()
                );
    }

    private static double perTickProbability(double perMinute) {
        double seconds = TICK_PERIOD.toMillis() / 1000.0;
        return 1.0 - Math.pow(1.0 - perMinute, seconds / 60.0);
    }

    private HamsterEvent maybeEventForSensor(SensorState s, double pEnterExit, double pFailure) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // Поломка датчика
        if (!s.failed && rnd.nextDouble() < pFailure) {
            s.failed = true;
            s.failureTicks = rnd.nextInt(5, 20); // 5–20 секунд «неисправен»
            return new SensorFailure(s.sensorId, 500 + rnd.nextInt(0, 10));
        }
        if (s.failed) {
            return null;
        }

        // Антиспам круток
        if (s.spinCooldown > 0) {
            s.spinCooldown--;
            return null;
        }

        // Вход/выход или крутка
        if (!s.inWheel) {
            if (rnd.nextDouble() < pEnterExit) {
                s.inWheel = true;
                return new HamsterEnter(s.hamsterId, s.wheelId);
            }
        } else {
            if (rnd.nextDouble() < pEnterExit) {
                s.inWheel = false;
                return new HamsterExit(s.hamsterId, s.wheelId);
            }
            int durationSec = 1 + rnd.nextInt(30);
            s.spinCooldown = Math.max(1, durationSec / 2);
            return new WheelSpin(s.wheelId, durationSec * 1000L);
        }
        return null;
    }

    private static final class SensorState {
        final String sensorId;
        final String wheelId;
        final String hamsterId;

        boolean failed = false;
        int failureTicks = 0;

        boolean inWheel = false;
        int spinCooldown = 0;

        SensorState(String sensorId, String wheelId, String hamsterId) {
            this.sensorId = sensorId;
            this.wheelId = wheelId;
            this.hamsterId = hamsterId;
        }
    }
}
