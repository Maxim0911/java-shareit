package ru.practicum.shareit.ServiceTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.UserServiceImpl;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private User user;
    private UserDto userDto;
    private UserDto updateDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("john@test.com");

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setName("John Doe");
        userDto.setEmail("john@test.com");

        updateDto = new UserDto();
        updateDto.setName("Jane Doe");
        updateDto.setEmail("jane@test.com");
    }

    // ==================== Тесты createUser ====================

    @Test
    void createUser_WithUniqueEmail_ShouldSuccess() {
        // given
        when(userRepository.findByEmail(userDto.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        // when
        UserDto result = userService.createUser(userDto);

        // then
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getName(), result.getName());
        assertEquals(user.getEmail(), result.getEmail());

        verify(userRepository).findByEmail(userDto.getEmail());
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(userDto.getName(), savedUser.getName());
        assertEquals(userDto.getEmail(), savedUser.getEmail());
    }

    @Test
    void createUser_WithDuplicateEmail_ShouldThrowException() {
        // given
        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setEmail(userDto.getEmail());

        when(userRepository.findByEmail(userDto.getEmail())).thenReturn(Optional.of(existingUser));

        // when & then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> userService.createUser(userDto));

        assertEquals("Email already exists: " + userDto.getEmail(), exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WithNullName_ShouldThrowException() {
        // given
        userDto.setName(null);

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(userDto));

        assertEquals("Name cannot be blank", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WithBlankName_ShouldThrowException() {
        // given
        userDto.setName("   ");

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(userDto));

        assertEquals("Name cannot be blank", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void createUser_WithNullEmail_ShouldThrowException() {
        // given
        userDto.setEmail(null);

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(userDto));

        assertEquals("Email cannot be blank", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void createUser_WithBlankEmail_ShouldThrowException() {
        // given
        userDto.setEmail("   ");

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(userDto));

        assertEquals("Email cannot be blank", exception.getMessage());
    }

    @Test
    void createUser_WithInvalidEmailFormat_ShouldThrowException() {
        // given
        userDto.setEmail("invalid-email");

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(userDto));

        assertEquals("Invalid email format", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void createUser_WithInvalidEmailFormat_MissingDomain() {
        // given
        userDto.setEmail("user@");

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(userDto));

        assertEquals("Invalid email format", exception.getMessage());
    }

    @Test
    void createUser_WithInvalidEmailFormat_MissingAt() {
        // given
        userDto.setEmail("user.com");

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(userDto));

        assertEquals("Invalid email format", exception.getMessage());
    }

    // ==================== Тесты getUserById ====================

    @Test
    void getUserById_ExistingUser_ShouldReturnUser() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        UserDto result = userService.getUserById(1L);

        // then
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getName(), result.getName());
        assertEquals(user.getEmail(), result.getEmail());

        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_NonExistingUser_ShouldThrowException() {
        // given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.getUserById(999L));

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository).findById(999L);
    }

    // ==================== Тесты getAllUsers ====================

    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        // given
        User user2 = new User();
        user2.setId(2L);
        user2.setName("Jane Doe");
        user2.setEmail("jane@test.com");

        List<User> users = List.of(user, user2);
        when(userRepository.findAll()).thenReturn(users);

        // when
        List<UserDto> results = userService.getAllUsers();

        // then
        assertNotNull(results);
        assertEquals(2, results.size());

        UserDto firstUser = results.get(0);
        assertEquals(user.getId(), firstUser.getId());
        assertEquals(user.getName(), firstUser.getName());

        UserDto secondUser = results.get(1);
        assertEquals(user2.getId(), secondUser.getId());
        assertEquals(user2.getName(), secondUser.getName());

        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_EmptyList_ShouldReturnEmptyList() {
        // given
        when(userRepository.findAll()).thenReturn(List.of());

        // when
        List<UserDto> results = userService.getAllUsers();

        // then
        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(userRepository).findAll();
    }

    // ==================== Тесты updateUser ====================

    @Test
    void updateUser_AllFields_ShouldUpdateSuccessfully() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(updateDto.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserDto result = userService.updateUser(1L, updateDto);

        // then
        assertNotNull(result);
        assertEquals(updateDto.getName(), result.getName());
        assertEquals(updateDto.getEmail(), result.getEmail());

        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals(updateDto.getName(), updatedUser.getName());
        assertEquals(updateDto.getEmail(), updatedUser.getEmail());
    }

    @Test
    void updateUser_OnlyName_ShouldUpdateOnlyName() {
        // given
        UserDto nameUpdateDto = new UserDto();
        nameUpdateDto.setName("New Name");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserDto result = userService.updateUser(1L, nameUpdateDto);

        // then
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals(user.getEmail(), result.getEmail());

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals("New Name", updatedUser.getName());
        assertEquals(user.getEmail(), updatedUser.getEmail());
    }

    @Test
    void updateUser_OnlyEmail_ShouldUpdateOnlyEmail() {
        // given
        UserDto emailUpdateDto = new UserDto();
        emailUpdateDto.setEmail("newemail@test.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("newemail@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserDto result = userService.updateUser(1L, emailUpdateDto);

        // then
        assertNotNull(result);
        assertEquals(user.getName(), result.getName());
        assertEquals("newemail@test.com", result.getEmail());

        verify(userRepository).findByEmail("newemail@test.com");
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals(user.getName(), updatedUser.getName());
        assertEquals("newemail@test.com", updatedUser.getEmail());
    }

    @Test
    void updateUser_NoChanges_ShouldReturnExistingUser() {
        // given
        UserDto emptyUpdateDto = new UserDto();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        UserDto result = userService.updateUser(1L, emptyUpdateDto);

        // then
        assertNotNull(result);
        assertEquals(user.getName(), result.getName());
        assertEquals(user.getEmail(), result.getEmail());

        verify(userRepository, never()).save(any());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void updateUser_UserNotFound_ShouldThrowException() {
        // given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.updateUser(999L, updateDto));

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void updateUser_WithBlankName_ShouldThrowException() {
        // given
        UserDto invalidUpdateDto = new UserDto();
        invalidUpdateDto.setName("   ");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.updateUser(1L, invalidUpdateDto));

        assertEquals("Name cannot be blank", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_WithBlankEmail_ShouldThrowException() {
        // given
        UserDto invalidUpdateDto = new UserDto();
        invalidUpdateDto.setEmail("   ");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.updateUser(1L, invalidUpdateDto));

        assertEquals("Email cannot be blank", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_WithInvalidEmailFormat_ShouldThrowException() {
        // given
        UserDto invalidUpdateDto = new UserDto();
        invalidUpdateDto.setEmail("invalid-email");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.updateUser(1L, invalidUpdateDto));

        assertEquals("Invalid email format", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void updateUser_WithDuplicateEmail_ShouldThrowException() {
        // given
        UserDto duplicateEmailDto = new UserDto();
        duplicateEmailDto.setEmail("existing@test.com");

        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setEmail("existing@test.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(existingUser));

        // when & then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> userService.updateUser(1L, duplicateEmailDto));

        assertEquals("Email already exists: existing@test.com", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_SameEmail_ShouldNotCheckDuplicate() {
        // given
        UserDto sameEmailDto = new UserDto();
        sameEmailDto.setEmail(user.getEmail());  // Тот же email

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // Не мокаем save!

        // when
        UserDto result = userService.updateUser(1L, sameEmailDto);

        // then
        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    // ==================== Тесты deleteUser ====================

    @Test
    void deleteUser_ShouldCallRepositoryDelete() {
        // given
        Long userId = 1L;
        doNothing().when(userRepository).deleteById(userId);

        // when
        userService.deleteUser(userId);

        // then
        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUser_WithNonExistingId_ShouldNotThrowException() {
        // given
        Long userId = 999L;
        doNothing().when(userRepository).deleteById(userId);

        // when & then
        assertDoesNotThrow(() -> userService.deleteUser(userId));

        verify(userRepository).deleteById(userId);
    }
}