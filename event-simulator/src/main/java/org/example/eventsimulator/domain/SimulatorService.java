package org.example.eventsimulator.domain;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.eventsimulator.eventDto.HamsterEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;


@Slf4j
@Service
public class SimulatorService {
    private final WebClient client;
    private final EventGenerator generator;
    @Value("${tracker-events-path}")
    private String eventsPath;
    private volatile int hamsterCount;
    private volatile int sensorCount;
    private final AtomicReference<Disposable> subscriptionRef = new AtomicReference<>();
    private static final int DEFAULT_PARALLELISM = 512;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    public SimulatorService(WebClient client, EventGenerator generator,
                            @Value("${hamster-count}") int hamsterCount,
                            @Value("${sensor-count}") int sensorCount) {
        this.client = client;
        this.generator = generator;
        this.hamsterCount = hamsterCount;
        this.sensorCount = sensorCount;
    }

    //Отправляет одно событие
    public Mono<Void> sendEvent(HamsterEvent event) {
        return client.post()
                .uri(eventsPath)
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .timeout(REQUEST_TIMEOUT)                // чтобы не повиснуть навсегда
                .doOnSuccess(r -> log.debug("Event sent: {}", event.getClass().getSimpleName()))
                .doOnError(ex -> log.warn("Event send failed: {}", ex.toString()))
                .onErrorResume(ex -> Mono.empty())       // не валим общий поток
                .then();
    }

    //Запуск генератора
    @PostConstruct
    public synchronized void start() {
        stop(); // гасим старую подписку, если была
        int parallelism = DEFAULT_PARALLELISM;

        Flux<HamsterEvent> events = generator.stream(hamsterCount, sensorCount); // <= 10k ev/s

        Disposable sub = sendStream(events, parallelism)
                .doOnSubscribe(s -> log.info("Streaming started: hamsters={}, sensors={}, parallelism={}",
                        hamsterCount, sensorCount, parallelism))
                .doOnTerminate(() -> log.info("Streaming stopped"))
                .subscribe(
                        null,
                        ex -> log.error("Stream terminated with error", ex)
                );

        subscriptionRef.set(sub);
    }

    public synchronized void applyConfig(int newHamsters, int newSensors) {
        this.hamsterCount = newHamsters;
        this.sensorCount = newSensors;
        start(); // перезапуск с новыми параметрами
    }

    public Mono<Void> sendStream(Flux<HamsterEvent> events, int parallelism) {
        int prefetch = parallelism * 2;
        return events
                .flatMap(this::sendEvent, parallelism, prefetch)
                .then();
    }

    public synchronized void stop() {
        Disposable old = subscriptionRef.getAndSet(null);
        if (old != null && !old.isDisposed()) {
            old.dispose();
        }
    }


}
