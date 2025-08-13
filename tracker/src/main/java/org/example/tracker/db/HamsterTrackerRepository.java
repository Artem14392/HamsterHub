package org.example.tracker.db;

import java.time.Instant;
import java.util.Map;

public interface HamsterTrackerRepository {

    //Кто в колесе
    String getOccupant(String wheelId);
    void setOccupant(String wheelId, String hamsterId);
    void clearOccupantIfMatches(String wheelId, String hamsterId);

    //Раунды за текущие сутки
    int addRounds(String hamsterId, int delta);
    int getRounds(String hamsterId);
    Map<String, Integer> getAllRoundsSnapshot();
    void resetDailyRounds();

    //Активность хомяков
    void updateLastActivity(String hamsterId, Instant when);
    Map<String, Instant> getAllLastActivity();
    boolean isInactivityAlerted(String hamsterId);
    void setInactivityAlerted(String hamsterId, boolean alerted);

    //Состояния датчиков
    boolean markSensorFailed(String sensorId, Instant since);
    void clearSensorFailure(String sensorId);
    Map<String, Instant> getAllSensorFailures();
    boolean isSensorAlerted(String sensorId);
    void setSensorAlerted(String sensorId, boolean alerted);
}
