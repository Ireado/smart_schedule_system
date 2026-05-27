package com.schedule.aisystem.service;

import com.schedule.aisystem.model.Course;
import com.schedule.aisystem.model.Room;
import com.schedule.aisystem.model.ScheduleEntry;
import java.util.List;
import java.util.Optional;

public interface AiSchedulingClient {

    Optional<List<ScheduleEntry>> generate(List<Course> courses, List<Room> rooms);
}

