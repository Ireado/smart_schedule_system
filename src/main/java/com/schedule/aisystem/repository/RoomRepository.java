package com.schedule.aisystem.repository;

import com.schedule.aisystem.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}

