package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {

        validateUserForCreation(userDto);

        userRepository.findByEmail(userDto.getEmail())
                .ifPresent(user -> {
                    throw new ConflictException("Email already exists: " + userDto.getEmail());
                });

        User user = UserMapper.toUser(userDto);
        User savedUser = userRepository.save(user);
        return UserMapper.toUserDto(savedUser);
    }

    @Override
    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDto updateUser(Long userId, UserDto userDto) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        boolean updated = false;

        if (userDto.getName() != null) {
            if (userDto.getName().isBlank()) {
                throw new ValidationException("Name cannot be blank");
            }
            existingUser.setName(userDto.getName());
            updated = true;
        }

        if (userDto.getEmail() != null) {
            if (userDto.getEmail().isBlank()) {
                throw new ValidationException("Email cannot be blank");
            }

            if (!isValidEmail(userDto.getEmail())) {
                throw new ValidationException("Invalid email format");
            }

            if (!userDto.getEmail().equals(existingUser.getEmail())) {
                userRepository.findByEmail(userDto.getEmail())
                        .ifPresent(user -> {
                            throw new ConflictException("Email already exists: " + userDto.getEmail());
                        });
                existingUser.setEmail(userDto.getEmail());
                updated = true;
            }
        }

        if (!updated) {
            return UserMapper.toUserDto(existingUser);
        }

        User updatedUser = userRepository.save(existingUser);
        return UserMapper.toUserDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    private void validateUserForCreation(UserDto userDto) {
        if (userDto.getName() == null || userDto.getName().isBlank()) {
            throw new ValidationException("Name cannot be blank");
        }

        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new ValidationException("Email cannot be blank");
        }

        if (!isValidEmail(userDto.getEmail())) {
            throw new ValidationException("Invalid email format");
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}