package org.example.eventsimulator.eventDto;

import lombok.Data;

@Data
public class SensorFailure extends HamsterEvent{
    private final String sensorId;
    private final int errorCode;

    public SensorFailure(String sensorId, int errorCode) {
        this.sensorId = sensorId;
        this.errorCode = errorCode;
    }


}
