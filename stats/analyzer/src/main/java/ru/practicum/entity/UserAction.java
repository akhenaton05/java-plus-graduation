package ru.practicum.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_action")
@Data
@EqualsAndHashCode
public class UserAction {
    @EmbeddedId
    private UserInteractionId id;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Embeddable
    @Getter
    @Setter
    public static class UserInteractionId {
        @Column(name = "user_id")
        private Long userId;

        @Column(name = "event_id")
        private Long eventId;
    }
}
