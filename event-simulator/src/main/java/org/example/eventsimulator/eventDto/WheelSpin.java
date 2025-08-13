package org.example.eventsimulator.eventDto;

import lombok.Data;

@Data
public class WheelSpin extends HamsterEvent{
    private final String wheelId;
    private final long durationMs;

    public WheelSpin(String wheelId, long durationMs) {
        this.wheelId = wheelId;
        this.durationMs = durationMs;
    }

}
