package org.example.eventsimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class EventSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventSimulatorApplication.class, args);
    }

}
