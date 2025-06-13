package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.entity.EventSimilarity;

import java.util.List;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarity.EventSimilarityId> {
    List<EventSimilarity> findByIdEventAOrIdEventB(Long eventA, Long eventB);
}