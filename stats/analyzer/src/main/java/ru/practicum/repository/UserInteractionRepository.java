package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.entity.UserAction;

import java.util.List;

public interface UserInteractionRepository extends JpaRepository<UserAction, UserAction.UserInteractionId> {
    List<UserAction> findByIdUserIdOrderByTimestampDesc(Long userId);
    List<UserAction> findByIdEventIdIn(List<Long> eventIds);
}