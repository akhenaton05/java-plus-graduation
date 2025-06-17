package ru.practicum.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "event_similarity")
@Data
@EqualsAndHashCode
public class EventSimilarity {
    @EmbeddedId
    private EventSimilarityId id;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Embeddable
    @Getter
    @Setter
    public static class EventSimilarityId {
        @Column(name = "event_a")
        private Long eventA;

        @Column(name = "event_b")
        private Long eventB;
    }
}
