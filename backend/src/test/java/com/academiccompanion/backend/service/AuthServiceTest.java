package com.academiccompanion.backend.service;

import com.academiccompanion.backend.dto.AuthResponse;
import com.academiccompanion.backend.dto.LoginRequest;
import com.academiccompanion.backend.model.Student;
import com.academiccompanion.backend.model.User;
import com.academiccompanion.backend.repository.StudentRepository;
import com.academiccompanion.backend.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private StudentRepository studentRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        studentRepository = Mockito.mock(StudentRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        jwtService = Mockito.mock(JwtService.class);

        authService = new AuthService(
                userRepository,
                studentRepository,
                passwordEncoder,
                jwtService
        );
    }

    private User createUser(String id, String name, String email, String passwordHash, User.Role role, String status) {
        User user = new User(name, email, passwordHash, role);
        user.setStatus(status);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ignored) {
        }
        return user;
    }

    @Test
    void testLogin_Success_Student() {
        ObjectId userId = new ObjectId();
        User studentUser = createUser(userId.toHexString(), "Aarav Sharma", "student@example.com", "hash", User.Role.STUDENT, "ACTIVE");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(studentUser));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(jwtService.generateToken(studentUser)).thenReturn("dummy-jwt-token");

        Student studentProfile = new Student(userId, "SAC001", "Aarav Sharma");
        when(studentRepository.findByUserId(userId)).thenReturn(Optional.of(studentProfile));

        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("secret123");

        AuthResponse response = authService.login(request);
        assertNotNull(response);
        assertEquals("STUDENT", response.getRole());
        assertEquals("dummy-jwt-token", response.getToken());
        assertEquals("Aarav Sharma", response.getFullName());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    void testLogin_Success_Teacher() {
        ObjectId userId = new ObjectId();
        User teacherUser = createUser(userId.toHexString(), "Dr. Priya Sharma", "priya.sharma@example.com", "hash", User.Role.TEACHER, "ACTIVE");

        when(userRepository.findByEmail("priya.sharma@example.com")).thenReturn(Optional.of(teacherUser));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(jwtService.generateToken(teacherUser)).thenReturn("teacher-jwt-token");
        when(studentRepository.findByUserId(userId)).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("priya.sharma@example.com");
        request.setPassword("secret123");

        AuthResponse response = authService.login(request);
        assertNotNull(response);
        assertEquals("TEACHER", response.getRole());
        assertEquals("teacher-jwt-token", response.getToken());
        assertEquals("Dr. Priya Sharma", response.getFullName());
    }

    @Test
    void testLogin_Success_Admin() {
        ObjectId userId = new ObjectId();
        User adminUser = createUser(userId.toHexString(), "System Administrator", "admin@example.com", "hash", User.Role.ADMIN, "ACTIVE");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(jwtService.generateToken(adminUser)).thenReturn("admin-jwt-token");
        when(studentRepository.findByUserId(userId)).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("admin@example.com");
        request.setPassword("secret123");

        AuthResponse response = authService.login(request);
        assertNotNull(response);
        assertEquals("ADMIN", response.getRole());
        assertEquals("admin-jwt-token", response.getToken());
        assertEquals("System Administrator", response.getFullName());
    }

    @Test
    void testLogin_Failure_InvalidPassword() {
        User user = createUser(new ObjectId().toHexString(), "User", "test@example.com", "hash", User.Role.STUDENT, "ACTIVE");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void testLogin_Failure_UserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("any");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void testLogin_Failure_InactiveAccount() {
        User user = createUser(new ObjectId().toHexString(), "User", "inactive@example.com", "hash", User.Role.STUDENT, "PENDING");
        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setEmail("inactive@example.com");
        request.setPassword("pass");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Account is not active", ex.getMessage());
    }

    @Test
    void testCurrentUser_ReturnsCorrectRole() {
        User user = createUser(new ObjectId().toHexString(), "Teacher", "t@example.com", "hash", User.Role.TEACHER, "ACTIVE");
        AuthResponse res = authService.currentUser(user);
        assertNotNull(res);
        assertEquals("TEACHER", res.getRole());
        assertEquals("t@example.com", res.getEmail());
    }
}
