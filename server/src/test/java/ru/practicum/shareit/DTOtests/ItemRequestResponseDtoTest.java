package ru.practicum.shareit.DTOtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@DisplayName("ItemRequestResponseDto JSON Test")
class ItemRequestResponseDtoTest {

    @Autowired
    private JacksonTester<ItemRequestResponseDto> json;

    @Autowired
    private ObjectMapper objectMapper;

    private ItemRequestResponseDto itemRequestResponseDto;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        ItemDto itemDto = ItemDto.builder()
                .id(1L)
                .name("Drill")
                .description("Powerful drill")
                .available(true)
                .ownerId(2L)
                .requestId(1L)
                .build();

        itemRequestResponseDto = ItemRequestResponseDto.builder()
                .id(1L)
                .description("Need a drill")
                .requestorId(1L)
                .created(now)
                .items(List.of(itemDto))
                .build();
    }

    @Test
    @DisplayName("Should serialize ItemRequestResponseDto to JSON")
    void testSerialize() throws IOException {
        JsonContent<ItemRequestResponseDto> result = json.write(itemRequestResponseDto);

        assertThat(result).hasJsonPathNumberValue("$.id");
        assertThat(result).hasJsonPathStringValue("$.description");
        assertThat(result).hasJsonPathNumberValue("$.requestorId");
        assertThat(result).hasJsonPathStringValue("$.created");
        assertThat(result).hasJsonPathArrayValue("$.items");

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Need a drill");
        assertThat(result).extractingJsonPathNumberValue("$.requestorId").isEqualTo(1);
        assertThat(result).extractingJsonPathNumberValue("$.items[0].id").isEqualTo(1);
    }

    @Test
    @DisplayName("Should deserialize JSON to ItemRequestResponseDto")
    void testDeserialize() throws IOException {
        String jsonContent = String.format(
                "{\"id\":1,\"description\":\"Need a drill\",\"requestorId\":1,\"created\":\"%s\"," +
                        "\"items\":[{\"id\":1,\"name\":\"Drill\",\"description\":\"Powerful drill\",\"available\":true,\"ownerId\":2,\"requestId\":1}]}",
                now.toString()
        );

        ItemRequestResponseDto deserialized = json.parseObject(jsonContent);

        assertThat(deserialized.getId()).isEqualTo(1L);
        assertThat(deserialized.getDescription()).isEqualTo("Need a drill");
        assertThat(deserialized.getRequestorId()).isEqualTo(1L);
        assertThat(deserialized.getItems()).hasSize(1);
        assertThat(deserialized.getItems().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should handle empty items list")
    void testEmptyItems() throws IOException {
        itemRequestResponseDto.setItems(List.of());

        JsonContent<ItemRequestResponseDto> result = json.write(itemRequestResponseDto);

        assertThat(result).hasJsonPathArrayValue("$.items");
        assertThat(result).extractingJsonPathArrayValue("$.items").isEmpty();
    }
}