
package com.academiccompanion.backend.service;

import com.academiccompanion.backend.dto.AuthResponse;
import com.academiccompanion.backend.dto.ForgotPasswordRequest;
import com.academiccompanion.backend.dto.LoginRequest;
import com.academiccompanion.backend.dto.RegisterRequest;
import com.academiccompanion.backend.dto.ResetPasswordRequest;
import com.academiccompanion.backend.model.Student;
import com.academiccompanion.backend.model.User;
import com.academiccompanion.backend.repository.StudentRepository;
import com.academiccompanion.backend.repository.UserRepository;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private static final int STUDENT_NUMBER_ATTEMPTS = 8;
    private static final SecureRandom STUDENT_NUMBER_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private MongoTemplate mongoTemplate;

    @org.springframework.beans.factory.annotation.Value("${app.env:development}")
    private String appEnv;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(
            UserRepository userRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse registerStudent(RegisterRequest request) {

        String fullName = request.getFullName().trim();
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists");
        }

        User user = new User(
                fullName,
                email,
                passwordEncoder.encode(request.getPassword()),
                User.Role.STUDENT
        );

        user.setStatus("ACTIVE");

        try {
            user = userRepository.save(user);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException(
                    "An account with this email already exists");
        }

        createStudentProfile(user, fullName);

        return toAuthResponse(
                user,
                jwtService.generateToken(user),
                "Student account created successfully");
    }

    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "Invalid email or password");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException(
                    "Account is not active");
        }

        if (user.getRole() == null) {
            throw new IllegalArgumentException(
                    "Account has no assigned role");
        }

        return toAuthResponse(
                user,
                jwtService.generateToken(user),
                "Login successful");
    }

    public AuthResponse loginStudent(LoginRequest request) {
        return login(request);
    }

    public AuthResponse currentUser(User user) {
        return toAuthResponse(user, null, null);
    }

    public Map<String, Object> forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);

        // Security: Always return the identical generic message to prevent email enumeration
        String genericMessage = "If an active account exists for this email, password reset instructions have been generated.";
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", genericMessage);

        if (email.isBlank() || mongoTemplate == null) {
            return response;
        }

        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || !"ACTIVE".equals(userOpt.get().getStatus())) {
            return response;
        }

        User user = userOpt.get();

        // Invalidate any previous unused tokens for this email
        mongoTemplate.updateMulti(
                new Query(Criteria.where("email").is(email).and("used").is(false)),
                new Update().set("used", true),
                "password_reset_tokens"
        );

        // Generate cryptographically secure random token (32 bytes = 64 hex chars)
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : randomBytes) {
            sb.append(String.format("%02x", b));
        }
        String resetToken = sb.toString();

        Date expiresAt = new Date(System.currentTimeMillis() + 15 * 60 * 1000L); // 15 mins

        Document tokenDoc = new Document()
                .append("userId", user.getId() != null && ObjectId.isValid(user.getId()) ? new ObjectId(user.getId()) : null)
                .append("email", email)
                .append("token", resetToken)
                .append("expiresAt", expiresAt)
                .append("used", false)
                .append("createdAt", new Date());

        mongoTemplate.insert(tokenDoc, "password_reset_tokens");

        boolean isProduction = "production".equalsIgnoreCase(appEnv) || "prod".equalsIgnoreCase(appEnv);
        if (isProduction) {
            response.put("emailDelivery", "queued");
        } else {
            response.put("emailDelivery", "simulated");
            response.put("resetToken", resetToken);
        }

        return response;
    }

    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);
        String token = request.getToken() == null ? "" : request.getToken().trim();
        String newPassword = request.getNewPassword();

        if (email.isBlank() || token.isBlank()) {
            throw new IllegalArgumentException("Email and reset token are required");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        if (mongoTemplate == null) {
            throw new IllegalStateException("Database service is unavailable");
        }

        // Validate token exists, matches email, not used, not expired
        Document tokenDoc = mongoTemplate.findOne(
                new Query(
                        Criteria.where("email").is(email)
                                .and("token").is(token)
                                .and("used").is(false)
                                .and("expiresAt").gt(new Date())
                ),
                Document.class,
                "password_reset_tokens"
        );

        if (tokenDoc == null) {
            throw new IllegalArgumentException("Invalid or expired password reset token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found or inactive"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        // Update password hash using BCrypt encoder
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(tokenDoc.getObjectId("_id"))),
                new Update().set("used", true).set("usedAt", new Date()),
                "password_reset_tokens"
        );

        return Map.of("message", "Password has been reset successfully. You may now sign in with your new password.");
    }

    private void createStudentProfile(User user, String fullName) {
        ObjectId userId = parseUserId(user.getId());
        DuplicateKeyException lastConflict = null;

        for (int attempt = 0; attempt < STUDENT_NUMBER_ATTEMPTS; attempt++) {
            String studentNumber = generateStudentNumber(user.getId());
            if (studentRepository.existsByStudentNumber(studentNumber)) {
                continue;
            }

            try {
                studentRepository.save(
                        new Student(userId, studentNumber, fullName));
                return;
            } catch (DuplicateKeyException e) {
                if (studentRepository.findByUserId(userId).isPresent()) {
                    return;
                }
                lastConflict = e;
            }
        }

        throw new IllegalStateException(
                "Account was created, but a unique student profile could not be saved",
                lastConflict);
    }

    private AuthResponse toAuthResponse(User user, String token, String message) {
        return AuthResponse.of(
                message,
                token,
                user.getId(),
                resolveFullName(user),
                user.getEmail(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getStatus());
    }

    private String resolveFullName(User user) {
        if (user.getId() != null) {
            try {
                return studentRepository.findByUserId(parseUserId(user.getId()))
                        .map(Student::getFullName)
                        .filter(name -> name != null && !name.isBlank())
                        .orElse(user.getFullName());
            } catch (IllegalArgumentException ignored) {
                return user.getFullName();
            }
        }

        return user.getFullName();
    }

    private ObjectId parseUserId(String userId) {
        if (userId == null || !ObjectId.isValid(userId)) {
            throw new IllegalArgumentException("Invalid user id");
        }
        return new ObjectId(userId);
    }

    private String generateStudentNumber(String userId) {
        String suffix = userId == null
                ? "000000"
                : userId.substring(Math.max(0, userId.length() - 6)).toUpperCase(Locale.ROOT);
        int token = STUDENT_NUMBER_RANDOM.nextInt(0x10000);
        return "SAC" + suffix + String.format("%04X", token);
    }
}
