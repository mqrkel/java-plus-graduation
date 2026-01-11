package ru.practicum.ewm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;


@Getter
@Setter
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "events")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 120)
    String title;

    @Column(nullable = false, length = 2000)
    String annotation;

    @Column(columnDefinition = "TEXT")
    String description;

    @ToString.Exclude
    @Column(name = "category_id", nullable = false)
    Long categoryId;

    @ToString.Exclude
    Long initiatorId;

    @Column(name = "event_date", nullable = false)
    LocalDateTime eventDate;

    @Builder.Default
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "published_on")
    LocalDateTime publishedOn;

    @ToString.Exclude
    @Column(name = "location_id", nullable = false)
    Long locationId;

    @Builder.Default
    @Column(nullable = false)
    Boolean paid = false;

    @Builder.Default
    @Column(name = "participant_limit", columnDefinition = "integer default 0")
    Integer participantLimit = 0;

    @Builder.Default
    @Column(name = "request_moderation", columnDefinition = "boolean default true")
    Boolean requestModeration = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    EventState state = EventState.PENDING;

    @Transient
    @Builder.Default
    Integer confirmedRequests = 0;

    @Transient
    @Builder.Default
    Double rating = 0.0;
}