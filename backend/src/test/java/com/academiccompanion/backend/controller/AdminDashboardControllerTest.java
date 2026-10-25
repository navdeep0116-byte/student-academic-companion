package com.academiccompanion.backend.controller;

import com.academiccompanion.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdminDashboardControllerTest {

    private AdminDashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminDashboardController();
    }

    private User createUser(String name, String email, User.Role role) {
        User user = new User(name, email, "hash", role);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, "test-id");
        } catch (Exception ignored) {
        }
        return user;
    }

    @Test
    void testDashboard_AdminAllowed() {
        User admin = createUser("Admin", "admin@example.com", User.Role.ADMIN);
        Authentication auth = new UsernamePasswordAuthenticationToken(admin, null);

        ResponseEntity<?> response = controller.dashboard(auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("CONNECTED", body.get("status"));
        assertEquals("ADMIN", body.get("role"));
    }

    @Test
    void testDashboard_TeacherDenied() {
        User teacher = createUser("Dr. Priya", "teacher@example.com", User.Role.TEACHER);
        Authentication auth = new UsernamePasswordAuthenticationToken(teacher, null);

        ResponseEntity<?> response = controller.dashboard(auth);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testDashboard_StudentDenied() {
        User student = createUser("Student", "student@example.com", User.Role.STUDENT);
        Authentication auth = new UsernamePasswordAuthenticationToken(student, null);

        ResponseEntity<?> response = controller.dashboard(auth);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testDashboard_UnauthenticatedDenied() {
        ResponseEntity<?> response = controller.dashboard(null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
