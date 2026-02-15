package ru.practicum.shareit.DTOtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@DisplayName("BookingResponseDto JSON Test")
class BookingResponseDtoTest {

    @Autowired
    private JacksonTester<BookingResponseDto> json;

    @Autowired
    private ObjectMapper objectMapper;

    private BookingResponseDto bookingResponseDto;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        UserDto booker = new UserDto(2L, "John Doe", "john@example.com");
        ItemDto item = ItemDto.builder()
                .id(1L)
                .name("Drill")
                .description("Powerful drill")
                .available(true)
                .ownerId(1L)
                .build();

        bookingResponseDto = BookingResponseDto.builder()
                .id(1L)
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .status(BookingStatus.APPROVED)
                .booker(booker)
                .item(item)
                .build();
    }

    @Test
    @DisplayName("Should serialize BookingResponseDto to JSON")
    void testSerialize() throws IOException {
        JsonContent<BookingResponseDto> result = json.write(bookingResponseDto);

        assertThat(result).hasJsonPathNumberValue("$.id");
        assertThat(result).hasJsonPathStringValue("$.start");
        assertThat(result).hasJsonPathStringValue("$.end");
        assertThat(result).hasJsonPathStringValue("$.status");
        assertThat(result).hasJsonPathMapValue("$.booker");
        assertThat(result).hasJsonPathMapValue("$.item");

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("APPROVED");
        assertThat(result).extractingJsonPathNumberValue("$.booker.id").isEqualTo(2);
        assertThat(result).extractingJsonPathNumberValue("$.item.id").isEqualTo(1);
    }

    @Test
    @DisplayName("Should deserialize JSON to BookingResponseDto")
    void testDeserialize() throws IOException {
        String jsonContent = String.format(
                "{\"id\":1,\"start\":\"%s\",\"end\":\"%s\",\"status\":\"APPROVED\"," +
                        "\"booker\":{\"id\":2,\"name\":\"John Doe\",\"email\":\"john@example.com\"}," +
                        "\"item\":{\"id\":1,\"name\":\"Drill\",\"description\":\"Powerful drill\",\"available\":true,\"ownerId\":1}}",
                now.plusDays(1).toString(),
                now.plusDays(2).toString()
        );

        BookingResponseDto deserialized = json.parseObject(jsonContent);

        assertThat(deserialized.getId()).isEqualTo(1L);
        assertThat(deserialized.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(deserialized.getBooker()).isNotNull();
        assertThat(deserialized.getBooker().getId()).isEqualTo(2L);
        assertThat(deserialized.getItem()).isNotNull();
        assertThat(deserialized.getItem().getId()).isEqualTo(1L);
    }
}