package     ru.practicum.shareit.DTOtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.user.dto.UserDto;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@DisplayName("UserDto JSON Test")
class UserDtoTest {

    @Autowired
    private JacksonTester<UserDto> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should serialize UserDto to JSON")
    void testSerialize() throws IOException {
        UserDto userDto = new UserDto(1L, "John Doe", "john@example.com");

        JsonContent<UserDto> result = json.write(userDto);

        assertThat(result).hasJsonPathNumberValue("$.id");
        assertThat(result).hasJsonPathStringValue("$.name");
        assertThat(result).hasJsonPathStringValue("$.email");

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.name").isEqualTo("John Doe");
        assertThat(result).extractingJsonPathStringValue("$.email").isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should deserialize JSON to UserDto")
    void testDeserialize() throws IOException {
        String jsonContent = "{\"id\":1,\"name\":\"John Doe\",\"email\":\"john@example.com\"}";

        UserDto userDto = json.parseObject(jsonContent);

        assertThat(userDto.getId()).isEqualTo(1L);
        assertThat(userDto.getName()).isEqualTo("John Doe");
        assertThat(userDto.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should handle null fields")
    void testNullFields() throws IOException {
        UserDto userDto = new UserDto(null, null, null);

        JsonContent<UserDto> result = json.write(userDto);

        assertThat(result).hasEmptyJsonPathValue("$.id");
        assertThat(result).hasEmptyJsonPathValue("$.name");
        assertThat(result).hasEmptyJsonPathValue("$.email");
    }
}