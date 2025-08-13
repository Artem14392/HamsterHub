package org.example.eventsimulator.eventDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SimulatorConfig(
        @Min(1) @Max(10_000) int hamsterCount,
        @Min(1) @Max(10_000) int sensorCount
) {}
