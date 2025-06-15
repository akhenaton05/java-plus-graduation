package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.entity.UserAction;

import java.util.List;

public interface UserInteractionRepository extends JpaRepository<UserAction, UserAction.UserInteractionId> {

    List<UserAction> findByIdUserIdOrderByTimestampDesc(Long userId);

    @Query("SELECT ua.id.eventId, SUM(ua.weight) FROM UserAction ua WHERE ua.id.eventId IN :eventIds " +
            "GROUP BY ua.id.eventId")
    List<Object[]> sumMaxWeightsByEventIds(List<Long> eventIds);
}