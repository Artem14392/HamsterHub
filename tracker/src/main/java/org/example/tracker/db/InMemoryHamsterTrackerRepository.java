package org.example.tracker.db;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class InMemoryHamsterTrackerRepository implements HamsterTrackerRepository {

    private final ConcurrentHashMap<String, String> wheelOccupants = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> roundsByHamster = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lastActivity = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> inactivityAlerted = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> sensorFailures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> sensorAlerted = new ConcurrentHashMap<>();

    @Override
    public String getOccupant(String wheelId) {
        if (wheelId == null) return null;
        return wheelOccupants.get(wheelId);
    }

    @Override
    public void setOccupant(String wheelId, String hamsterId) {
        if (wheelId == null || hamsterId == null) return;
        wheelOccupants.put(wheelId, hamsterId);
    }

    @Override
    public void clearOccupantIfMatches(String wheelId, String hamsterId) {
        if (wheelId == null || hamsterId == null) return;
        wheelOccupants.compute(wheelId, (k, current) -> Objects.equals(current, hamsterId) ? null : current);
    }

    @Override
    public int addRounds(String hamsterId, int delta) {
        if (hamsterId == null || delta == 0) {
            return hamsterId == null ? 0 : getRounds(hamsterId);
        }
        AtomicInteger counter = roundsByHamster.computeIfAbsent(hamsterId, k -> new AtomicInteger(0));
        return counter.addAndGet(delta);
    }

    @Override
    public int getRounds(String hamsterId) {
        if (hamsterId == null) return 0;
        AtomicInteger ai = roundsByHamster.get(hamsterId);
        return ai == null ? 0 : ai.get();
    }

    @Override
    public Map<String, Integer> getAllRoundsSnapshot() {
        Map<String, Integer> copy = new HashMap<>(roundsByHamster.size());
        roundsByHamster.forEach((k, v) -> copy.put(k, v.get()));
        return Collections.unmodifiableMap(copy);
    }

    @Override
    public void resetDailyRounds() {
        roundsByHamster.values().forEach(ai -> ai.set(0));
    }

    @Override
    public void updateLastActivity(String hamsterId, Instant when) {
        if (hamsterId == null || when == null) return;
        lastActivity.put(hamsterId, when);
    }

    @Override
    public Map<String, Instant> getAllLastActivity() {
        return Collections.unmodifiableMap(new HashMap<>(lastActivity));
    }

    @Override
    public boolean isInactivityAlerted(String hamsterId) {
        if (hamsterId == null) return false;
        return inactivityAlerted.getOrDefault(hamsterId, false);
    }

    @Override
    public void setInactivityAlerted(String hamsterId, boolean alerted) {
        if (hamsterId == null) return;
        if (!alerted) {
            inactivityAlerted.remove(hamsterId);
        } else {
            inactivityAlerted.put(hamsterId, true);
        }
    }

    @Override
    public boolean markSensorFailed(String sensorId, Instant since) {
        if (sensorId == null || since == null) return false;
        return sensorFailures.putIfAbsent(sensorId, since) == null; // true, если записали впервые
    }

    @Override
    public void clearSensorFailure(String sensorId) {
        if (sensorId == null) return;
        sensorFailures.remove(sensorId);
    }

    @Override
    public Map<String, Instant> getAllSensorFailures() {
        return Collections.unmodifiableMap(new HashMap<>(sensorFailures));
    }

    @Override
    public boolean isSensorAlerted(String sensorId) {
        if (sensorId == null) return false;
        return sensorAlerted.getOrDefault(sensorId, false);
    }

    @Override
    public void setSensorAlerted(String sensorId, boolean alerted) {
        if (sensorId == null) return;
        if (!alerted) {
            sensorAlerted.remove(sensorId);
        } else {
            sensorAlerted.put(sensorId, true);
        }
    }
}
