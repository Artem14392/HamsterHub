package org.example.tracker.domain;

import org.springframework.stereotype.Service;

@Service
public class HamsterMailService implements AlertService{
    @Override
    public void sendAlert(String message) {
        // Реальная реализация не важна
    }
}
