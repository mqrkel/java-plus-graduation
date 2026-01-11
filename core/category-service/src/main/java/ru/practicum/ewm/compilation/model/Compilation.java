package ru.practicum.ewm.compilation.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "compilations")
public class Compilation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 55)
    private String title;

    @Column(nullable = false)
    private Boolean pinned;

    /**
     * Просто набор ID событий, которые входят в подборку.
     * JPA сама мапит это на таблицу compilation_events (compilation_id, event_id).
     */
    @ElementCollection
    @CollectionTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id")
    )
    @Builder.Default
    @Column(name = "event_id")
    private Set<Long> events = new HashSet<>();
}