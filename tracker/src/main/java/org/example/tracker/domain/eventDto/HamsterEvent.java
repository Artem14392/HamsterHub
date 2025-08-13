package org.example.tracker.domain.eventDto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

//события от датчиков
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HamsterEnter.class, name = "HamsterEnter"),
        @JsonSubTypes.Type(value = HamsterExit.class, name = "HamsterExit"),
        @JsonSubTypes.Type(value = SensorFailure.class, name = "SensorFailure"),
        @JsonSubTypes.Type(value = WheelSpin.class, name = "WheelSpin")
})
public abstract class HamsterEvent {
}
