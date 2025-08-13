package org.example.tracker.api;

import lombok.extern.slf4j.Slf4j;
import org.example.tracker.domain.eventDto.HamsterEvent;
import org.example.tracker.domain.HamsterTrackerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/tracker")
public class HamsterEventController {

    private final HamsterTrackerService trackerService;

    public HamsterEventController(HamsterTrackerService trackerService) {
        this.trackerService = trackerService;
    }

    @PostMapping("/events")
    public Mono<ResponseEntity<Void>> receiveEvent(@RequestBody Mono<HamsterEvent> eventMono) {
        return eventMono
                .doOnNext(event -> {
                    log.info("Event received: {}", event);
                    trackerService.accept(event);
                })
                .then(Mono.fromSupplier(() -> ResponseEntity.ok().build()));
    }



}
