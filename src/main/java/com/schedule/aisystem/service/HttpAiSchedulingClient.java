package com.schedule.aisystem.service;

import com.schedule.aisystem.config.AiProperties;
import com.schedule.aisystem.model.Course;
import com.schedule.aisystem.model.Room;
import com.schedule.aisystem.model.ScheduleEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HttpAiSchedulingClient implements AiSchedulingClient {

    private final AiProperties aiProperties;

    public HttpAiSchedulingClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Override
    public Optional<List<ScheduleEntry>> generate(List<Course> courses, List<Room> rooms) {
        if (!aiProperties.isEnabled() || aiProperties.getEndpoint() == null || aiProperties.getEndpoint().isBlank()) {
            return Optional.empty();
        }

        return Optional.empty();
    }
}

