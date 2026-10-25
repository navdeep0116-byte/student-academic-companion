package com.academiccompanion.backend.service;

import com.academiccompanion.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "this-is-a-very-secure-32-byte-long-secret-key!!";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3600000L);
    }

    private User createUser(String id, String email, User.Role role) {
        User user = new User("Test User", email, "hash", role);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ignored) {
        }
        return user;
    }

    @Test
    void testGenerateToken_IncludesRoleClaim_Student() {
        User student = createUser("student-123", "student@example.com", User.Role.STUDENT);
        String token = jwtService.generateToken(student);
        assertNotNull(token);
        assertEquals("student-123", jwtService.extractUserId(token));
        assertEquals("STUDENT", jwtService.extractRole(token));
        assertTrue(jwtService.isTokenValid(token, student));
    }

    @Test
    void testGenerateToken_IncludesRoleClaim_Teacher() {
        User teacher = createUser("teacher-456", "teacher@example.com", User.Role.TEACHER);
        String token = jwtService.generateToken(teacher);
        assertNotNull(token);
        assertEquals("teacher-456", jwtService.extractUserId(token));
        assertEquals("TEACHER", jwtService.extractRole(token));
        assertTrue(jwtService.isTokenValid(token, teacher));
    }

    @Test
    void testGenerateToken_IncludesRoleClaim_Admin() {
        User admin = createUser("admin-789", "admin@example.com", User.Role.ADMIN);
        String token = jwtService.generateToken(admin);
        assertNotNull(token);
        assertEquals("admin-789", jwtService.extractUserId(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
        assertTrue(jwtService.isTokenValid(token, admin));
    }

    @Test
    void testTokenValidation_FailsForDifferentUser() {
        User student = createUser("user-1", "u1@example.com", User.Role.STUDENT);
        User different = createUser("user-2", "u2@example.com", User.Role.STUDENT);
        String token = jwtService.generateToken(student);
        assertFalse(jwtService.isTokenValid(token, different));
    }
}
