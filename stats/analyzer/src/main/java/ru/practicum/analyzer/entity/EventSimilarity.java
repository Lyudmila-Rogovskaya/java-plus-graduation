package ru.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "event_similarity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(EventSimilarityId.class)
public class EventSimilarity {

    @Id
    @Column(name = "event_a", nullable = false)
    private Long eventA;

    @Id
    @Column(name = "event_b", nullable = false)
    private Long eventB;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private Instant updated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updated = Instant.now();
    }

}
