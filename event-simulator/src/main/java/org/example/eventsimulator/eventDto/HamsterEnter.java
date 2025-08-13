package org.example.eventsimulator.eventDto;

import lombok.Data;

@Data
public class HamsterEnter extends HamsterEvent{
    private final String hamsterId;
    private final String wheelId;

    public HamsterEnter(String hamsterId, String wheelId) {
        this.hamsterId = hamsterId;
        this.wheelId = wheelId;
    }


}
