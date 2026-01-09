package ru.practicum.statsdto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HitDto {

    @NotBlank
    @JsonProperty("app")
    private String service;

    @NotBlank
    // supported formats: /path or /the/path or /the/path/123 ...
    @Pattern(regexp = "^(/\\w+)+$",
            message = "Invalid uri format")
    private String uri;

    @NotBlank
    private String ip;

    @JsonProperty("timestamp")
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateTime;
}
