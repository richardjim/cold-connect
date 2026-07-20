package com.coldconnect.service;

import com.coldconnect.entity.AppEvent;
import com.coldconnect.repository.AppEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventService {

    private final AppEventRepository eventRepository;

    public EventService(AppEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public AppEvent logEvent(Long userId, String eventName, String screenId,
                             String entityType, String entityId, String region,
                             String role, String networkState, String deviceInfo,
                             String featureFlagState) {
        AppEvent event = new AppEvent();
        event.setUserId(userId);
        event.setEventName(eventName);
        event.setScreenId(screenId);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setRegion(region);
        event.setRole(role);
        event.setNetworkState(networkState);
        event.setDeviceInfo(deviceInfo);
        event.setFeatureFlagState(featureFlagState);
        return eventRepository.save(event);
    }

    public List<AppEvent> getUserEvents(Long userId) {
        return eventRepository.findByUserIdOrderByOccurredAtDesc(userId);
    }

    public List<AppEvent> getEventsByName(String eventName) {
        return eventRepository.findByEventNameOrderByOccurredAtDesc(eventName);
    }
}