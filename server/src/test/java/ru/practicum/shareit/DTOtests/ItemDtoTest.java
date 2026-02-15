package ru.practicum.shareit.DTOtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.item.dto.CommentResponseDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@DisplayName("ItemDto JSON Test")
class ItemDtoTest {

    @Autowired
    private JacksonTester<ItemDto> json;

    @Autowired
    private ObjectMapper objectMapper;

    private ItemDto itemDto;
    private CommentResponseDto comment;
    private BookingShortDto lastBooking;
    private BookingShortDto nextBooking;

    @BeforeEach
    void setUp() {
        comment = CommentResponseDto.builder()
                .id(1L)
                .text("Great item!")
                .authorName("John Doe")
                .created(LocalDateTime.now())
                .build();

        lastBooking = BookingShortDto.builder()
                .id(1L)
                .bookerId(2L)
                .build();

        nextBooking = BookingShortDto.builder()
                .id(2L)
                .bookerId(3L)
                .build();

        itemDto = ItemDto.builder()
                .id(1L)
                .name("Drill")
                .description("Powerful drill")
                .available(true)
                .ownerId(1L)
                .requestId(2L)
                .lastBooking(lastBooking)
                .nextBooking(nextBooking)
                .comments(List.of(comment))
                .build();
    }

    @Test
    @DisplayName("Should serialize ItemDto to JSON")
    void testSerialize() throws IOException {
        JsonContent<ItemDto> result = json.write(itemDto);

        assertThat(result).hasJsonPathNumberValue("$.id");
        assertThat(result).hasJsonPathStringValue("$.name");
        assertThat(result).hasJsonPathStringValue("$.description");
        assertThat(result).hasJsonPathBooleanValue("$.available");
        assertThat(result).hasJsonPathNumberValue("$.ownerId");
        assertThat(result).hasJsonPathNumberValue("$.requestId");
        assertThat(result).hasJsonPathMapValue("$.lastBooking");
        assertThat(result).hasJsonPathMapValue("$.nextBooking");
        assertThat(result).hasJsonPathArrayValue("$.comments");

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.name").isEqualTo("Drill");
        assertThat(result).extractingJsonPathBooleanValue("$.available").isTrue();
        assertThat(result).extractingJsonPathNumberValue("$.lastBooking.id").isEqualTo(1);
        assertThat(result).extractingJsonPathNumberValue("$.comments[0].id").isEqualTo(1);
    }

    @Test
    @DisplayName("Should deserialize JSON to ItemDto")
    void testDeserialize() throws IOException {
        String jsonContent = "{\"id\":1,\"name\":\"Drill\",\"description\":\"Powerful drill\"," +
                "\"available\":true,\"ownerId\":1,\"requestId\":2," +
                "\"lastBooking\":{\"id\":1,\"bookerId\":2}," +
                "\"nextBooking\":{\"id\":2,\"bookerId\":3}," +
                "\"comments\":[{\"id\":1,\"text\":\"Great item!\",\"authorName\":\"John Doe\"}]}";

        ItemDto deserialized = json.parseObject(jsonContent);

        assertThat(deserialized.getId()).isEqualTo(1L);
        assertThat(deserialized.getName()).isEqualTo("Drill");
        assertThat(deserialized.getAvailable()).isTrue();
        assertThat(deserialized.getLastBooking()).isNotNull();
        assertThat(deserialized.getLastBooking().getId()).isEqualTo(1L);
        assertThat(deserialized.getComments()).hasSize(1);
        assertThat(deserialized.getComments().get(0).getText()).isEqualTo("Great item!");
    }

    @Test
    @DisplayName("Should handle null nested objects")
    void testNullNestedObjects() throws IOException {
        ItemDto itemWithNulls = ItemDto.builder()
                .id(1L)
                .name("Drill")
                .description("Powerful drill")
                .available(true)
                .ownerId(1L)
                .build();

        JsonContent<ItemDto> result = json.write(itemWithNulls);

        assertThat(result).hasEmptyJsonPathValue("$.requestId");
        assertThat(result).hasEmptyJsonPathValue("$.lastBooking");
        assertThat(result).hasEmptyJsonPathValue("$.nextBooking");
        assertThat(result).hasEmptyJsonPathValue("$.comments");
    }
}