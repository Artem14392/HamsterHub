package org.example.tracker.domain;

import lombok.Getter;

@Getter
public class HamsterStats {
    private final String hamsterId;
    private final int totalRounds;
    private final boolean active;

    public HamsterStats(String hamsterId, int totalRounds, boolean active) {
        this.hamsterId = hamsterId;
        this.totalRounds = totalRounds;
        this.active = active;
    }
}
