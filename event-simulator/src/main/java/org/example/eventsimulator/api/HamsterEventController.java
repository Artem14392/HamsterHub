package org.example.eventsimulator.api;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.eventsimulator.eventDto.SimulatorConfig;
import org.example.eventsimulator.domain.SimulatorService;
import org.example.eventsimulator.eventDto.HamsterEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/simulator")
@Slf4j
public class HamsterEventController {
    private final SimulatorService simulatorService;
    public HamsterEventController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @PostMapping("/config")
    public Mono<ResponseEntity<String>> setConfig(@Valid @RequestBody SimulatorConfig config) {
        simulatorService.applyConfig(config.hamsterCount(), config.sensorCount());
        return Mono.just(ResponseEntity.ok("Hamster configuration updated"));
    }

    //    - POST /simulator/events — отправляет одно событие HamsterEvent.
    @PostMapping("/events")
    public Mono<ResponseEntity<HamsterEvent>> receivedEvent(@RequestBody Mono<HamsterEvent> body) {
        return body
                .flatMap(evt -> simulatorService.sendEvent(evt).thenReturn(evt))
                .doOnSuccess(evt -> log.info("event sent: {}", evt))          // лог — побочный эффект ок
                .doOnError(ex -> log.warn("event send failed: {}", ex.toString()))
                .map(ResponseEntity::ok);
    }

}
