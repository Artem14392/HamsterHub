package org.example.tracker.domain.eventDto;

import lombok.Data;

@Data
public class HamsterExit extends HamsterEvent {
    private final String hamsterId;
    private final String wheelId;

    public HamsterExit(String hamsterId, String wheelId) {
        this.hamsterId = hamsterId;
        this.wheelId = wheelId;
    }

}
