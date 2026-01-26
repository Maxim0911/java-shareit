package ru.practicum.shareit.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemRequest {
    private Long id;

    @NotBlank(message = "Description cannot be blank")
    private String description;

    private String requestion;

    private LocalDateTime created;
}
