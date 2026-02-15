package ru.practicum.shareit.ControllerTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.GlobalExceptionHandler;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.UserController;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("UserController MVC Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserDto userDto;
    private UserDto createdUserDto;

    @BeforeEach
    void setUp() {
        userDto = new UserDto();
        userDto.setName("John Doe");
        userDto.setEmail("john@example.com");

        createdUserDto = new UserDto();
        createdUserDto.setId(1L);
        createdUserDto.setName("John Doe");
        createdUserDto.setEmail("john@example.com");
    }

    @Test
    @DisplayName("POST /users - should create user successfully")
    void createUser_Success() throws Exception {
        when(userService.createUser(any(UserDto.class))).thenReturn(createdUserDto);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService, times(1)).createUser(any(UserDto.class));
    }

    @Test
    @DisplayName("POST /users - should return 400 when name is blank")
    void createUser_BlankName_ReturnsBadRequest() throws Exception {
        userDto.setName("");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /users - should return 400 when email is invalid")
    void createUser_InvalidEmail_ReturnsBadRequest() throws Exception {
        userDto.setEmail("invalid-email");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("GET /users/{userId} - should return user by id")
    void getUserById_Success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(createdUserDto);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("GET /users/{userId} - should return 400 when userId is negative")
    void getUserById_NegativeId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/users/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(userService, never()).getUserById(any());
    }

    @Test
    @DisplayName("GET /users - should return all users")
    void getAllUsers_Success() throws Exception {
        UserDto user2 = new UserDto(2L, "Jane Smith", "jane@example.com");
        List<UserDto> users = Arrays.asList(createdUserDto, user2);

        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("PATCH /users/{userId} - should update user successfully")
    void updateUser_Success() throws Exception {
        UserDto updateDto = new UserDto();
        updateDto.setName("Updated Name");
        updateDto.setEmail("updated@example.com");

        UserDto updatedUser = new UserDto(1L, "Updated Name", "updated@example.com");

        when(userService.updateUser(eq(1L), any(UserDto.class))).thenReturn(updatedUser);

        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));

        verify(userService, times(1)).updateUser(eq(1L), any(UserDto.class));
    }

    @Test
    @DisplayName("PATCH /users/{userId} - should return 400 when userId is negative")
    void updateUser_NegativeId_ReturnsBadRequest() throws Exception {
        UserDto updateDto = new UserDto();
        updateDto.setName("Updated Name");

        mockMvc.perform(patch("/users/-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    @DisplayName("DELETE /users/{userId} - should delete user successfully")
    void deleteUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    @DisplayName("DELETE /users/{userId} - should return 400 when userId is negative")
    void deleteUser_NegativeId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(delete("/users/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(userService, never()).deleteUser(any());
    }

    @Test
    @DisplayName("GET /users/{userId} - should return 404 when user not found")
    void getUserById_NotFound_Returns404() throws Exception {
        when(userService.getUserById(999L)).thenThrow(new NotFoundException("User not found with id: 999"));

        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found with id: 999"));

        verify(userService, times(1)).getUserById(999L);
    }
}