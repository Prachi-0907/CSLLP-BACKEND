package com.configserverllp.csllp_learning_platform.user_service.service.impl;

import com.configserverllp.csllp_learning_platform.user_service.dto.LoginRequest;
import com.configserverllp.csllp_learning_platform.user_service.dto.UserRequest;
import com.configserverllp.csllp_learning_platform.user_service.dto.UserResponse;
import com.configserverllp.csllp_learning_platform.user_service.dto.UserSearchResponse;
import com.configserverllp.csllp_learning_platform.user_service.entity.Otp;
import com.configserverllp.csllp_learning_platform.user_service.entity.User;
import com.configserverllp.csllp_learning_platform.user_service.exception.BadRequestException;
import com.configserverllp.csllp_learning_platform.user_service.exception.ResourceNotFoundException;
import com.configserverllp.csllp_learning_platform.user_service.repository.OtpRepository;
import com.configserverllp.csllp_learning_platform.user_service.repository.UserRepository;
import com.configserverllp.csllp_learning_platform.user_service.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    // Add this field in UserServiceImpl class
    private final OtpRepository otpRepository;

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    private final String NOTIFICATION_URL = "http://localhost:8089/api/notifications/send-email";

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, RestTemplate restTemplate, OtpRepository otpRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
        this.otpRepository = otpRepository;
    }

    @Override
    public User createUser(UserRequest req, Long creatorId, String creatorRole) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Validate Manager if managerId provided
        Optional.ofNullable(req.getManagerId()).ifPresent(mid -> {
            User m = userRepository.findById(mid)
                    .orElseThrow(() -> new BadRequestException("Manager not found"));
            if (!"MANAGER".equalsIgnoreCase(m.getRole()))
                throw new BadRequestException("Selected manager is not a manager");
        });

        // Manager can only create employees
        if ("MANAGER".equalsIgnoreCase(creatorRole) && !"EMPLOYEE".equalsIgnoreCase(req.getRole())) {
            throw new BadRequestException("Manager can only create EMPLOYEE role");
        }

        User u = new User();
        u.setEmail(req.getEmail());
        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setRole(req.getRole());
        u.setStatus("ACTIVE");
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());

        // Set managerId for manager-created employee
        if ("MANAGER".equalsIgnoreCase(creatorRole)) u.setManagerId(creatorId);
        Optional.ofNullable(req.getManagerId()).ifPresent(u::setManagerId);

        User saved = userRepository.save(u);

        // Send notification email
        sendEmailViaNotificationService(saved.getEmail(), req.getPassword());

        // --- NEW: Assign mandatory courses for EMPLOYEE ---
        if ("EMPLOYEE".equalsIgnoreCase(u.getRole())) {
            try {
                List<Long> mandatoryCourseIds = restTemplate.getForObject(
                        "http://course-service/api/courses/mandatory", List.class);
                if (mandatoryCourseIds != null) {
                    for (Long courseId : mandatoryCourseIds) {
                        Map<String, Object> request = Map.of(
                                "employeeId", u.getId(),
                                "courseId", courseId
                        );
                        restTemplate.postForObject(
                                "http://employee-course-service/api/assign", request, Void.class
                        );
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Optional: Log error but allow user creation
            }
        }

        return saved;
    }

    private void sendEmailViaNotificationService(String to, String rawPassword) {
        try {
            Map<String, String> request = Map.of(
                    "to", to,
                    "subject", "Your account credentials - CSLLP",
                    "message", "Your account has been created.\nUsername: " + to +
                            "\nPassword: " + rawPassword +
                            "\nPlease change your password after first login."
            );
            restTemplate.postForObject(NOTIFICATION_URL, request, String.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public User updateUser(Long id, UserRequest update) {
        User existing = getUserById(id);
        existing.setFirstName(update.getFirstName());
        existing.setLastName(update.getLastName());
        existing.setRole(update.getRole());
        existing.setManagerId(update.getManagerId());
        existing.setUpdatedAt(LocalDateTime.now());
        Optional.ofNullable(update.getPassword()).filter(p -> !p.trim().isEmpty())
                .ifPresent(p -> existing.setPassword(passwordEncoder.encode(p)));
        return userRepository.save(existing);
    }

    @Override
    public void softDeleteUser(Long id) {
        User u = getUserById(id);
        u.setStatus("INACTIVE");
        userRepository.save(u);
    }

    @Override
    public User activateUser(Long id) {
        User u = getUserById(id);
        u.setStatus("ACTIVE"); // ✅ force status change
        u.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(u);
    }

    @Override
    public List<User> getUsersByManagerId(Long managerId) {
        return userRepository.findByManagerId(managerId);
    }

    @Override
    public List<User> getUsersByRole(String role) {
        if ("ALL".equalsIgnoreCase(role)) return userRepository.findAll();
        return userRepository.findByRole(role);
    }

    @Override
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    @Override
    public User login(LoginRequest req) {
        User u = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!"ACTIVE".equalsIgnoreCase(u.getStatus()))
            throw new BadRequestException("User not active");

        if (!u.getRole().equalsIgnoreCase(req.getRole()))
            throw new BadRequestException("User does not have role: " + req.getRole());

//        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
//            throw new BadRequestException("Invalid credentials");
//        }
        boolean passwordMatches =
                passwordEncoder.matches(req.getPassword(), u.getPassword())
                        ||
                        req.getPassword().equals(u.getPassword());

        if (!passwordMatches) {
            throw new BadRequestException("Invalid credentials");
        }

        return u;
    }

    @Override
    public UserResponse getProfile(Long userId) {
        User u = getUserById(userId);
        return UserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .role(u.getRole())
                .managerId(u.getManagerId())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .profilePhotoUrl(u.getProfilePhotoUrl()) // assuming added in User entity
                .build();
    }

    @Override
    public UserResponse updateProfile(Long userId, UserRequest updateReq) {
        User u = getUserById(userId);
        if (updateReq.getFirstName() != null) u.setFirstName(updateReq.getFirstName());
        if (updateReq.getLastName() != null) u.setLastName(updateReq.getLastName());
        if (updateReq.getPassword() != null && !updateReq.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(updateReq.getPassword()));
        }
        if (updateReq.getProfilePhotoUrl() != null) {
            u.setProfilePhotoUrl(updateReq.getProfilePhotoUrl());
        }
        u.setUpdatedAt(LocalDateTime.now());
        userRepository.save(u);
        return getProfile(userId);
    }

    @Override
    public int getTotalEmployees() {
        return userRepository.findByRole("EMPLOYEE").size();
    }

    @Override
    public int getTotalCourses() {
        // TODO: Call Course Service to fetch total courses
        return 0;
    }

    @Override
    public int getCompletedCourses() {
        // TODO: Call Course Service to fetch completed courses
        return 0;
    }

    // NEW METHOD IMPLEMENTATION
    @Override
    public Long getUserCountByRole(String role) {
        if ("ALL".equalsIgnoreCase(role)) {
            return userRepository.count();
        }
        return userRepository.countByRole(role);
    }

    // NEW METHOD IMPLEMENTATION - Search Users
    @Override
    public List<User> searchUsers(String query) {
        return userRepository.searchUsers(query);
    }

    // Add these methods at the end of UserServiceImpl class

    @Override
    @Transactional
    public String generateOtp(String email) {
        // Check if user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User with this email does not exist"));

        // Check rate limiting (max 3 OTPs in last 10 minutes)
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        long recentOtpCount = otpRepository.countRecentOtps(email, tenMinutesAgo);

        if (recentOtpCount >= 3) {
            throw new BadRequestException("Too many OTP requests. Please try again after 10 minutes.");
        }

        // Invalidate previous OTPs for this email
        otpRepository.invalidatePreviousOtps(email);

        // Generate 6-digit OTP
        String otpCode = generateRandomOtp();

        // Create and save OTP
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setOtpCode(otpCode);
        otp.setCreatedAt(LocalDateTime.now());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(10)); // OTP valid for 10 minutes
        otp.setUsed(false);

        otpRepository.save(otp);

        // Send OTP via email
        sendOtpEmail(email, otpCode);

        return otpCode;
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String otp) {
        Optional<Otp> otpOptional = otpRepository.findByEmailAndOtpCodeAndUsedFalse(email, otp);

        if (otpOptional.isEmpty()) {
            throw new BadRequestException("Invalid OTP");
        }

        Otp otpEntity = otpOptional.get();

        if (otpEntity.isExpired()) {
            throw new BadRequestException("OTP has expired");
        }

        return true;
    }

    @Override
    public boolean resetPassword(String email, String otp, String newPassword) {
        // Verify OTP first
        verifyOtp(email, otp);

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark OTP as used
        Optional<Otp> otpOptional = otpRepository.findByEmailAndOtpCodeAndUsedFalse(email, otp);
        if (otpOptional.isPresent()) {
            Otp otpEntity = otpOptional.get();
            otpEntity.setUsed(true);
            otpRepository.save(otpEntity);
        }

        // Send password reset confirmation email
        sendPasswordResetConfirmationEmail(email);

        return true;
    }

    private String generateRandomOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // Generates 6-digit OTP
        return String.valueOf(otp);
    }

    private void sendOtpEmail(String email, String otpCode) {
        try {
            Map<String, String> request = Map.of(
                    "to", email,
                    "subject", "Password Reset OTP - CSLLP",
                    "message", "Your OTP for password reset is: " + otpCode +
                            "\nThis OTP is valid for 10 minutes." +
                            "\nIf you didn't request this, please ignore this email."
            );
            restTemplate.postForObject(NOTIFICATION_URL, request, String.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new BadRequestException("Failed to send OTP email");
        }
    }

    private void sendPasswordResetConfirmationEmail(String email) {
        try {
            Map<String, String> request = Map.of(
                    "to", email,
                    "subject", "Password Reset Successful - CSLLP",
                    "message", "Your password has been successfully reset." +
                            "\nIf you didn't make this change, please contact support immediately."
            );
            restTemplate.postForObject(NOTIFICATION_URL, request, String.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            // Don't throw exception here as password is already reset
        }
    }
    @Override
    public List<UserSearchResponse> searchUsersForAssignments(String query, String role) {
        List<User> users = searchUsers(query);

        return users.stream()
                .filter(user -> role == null || user.getRole().equalsIgnoreCase(role))
                .map(user -> UserSearchResponse.builder()
                        .id(user.getId())
                        .employeeId("EMP" + user.getId()) // Generate employee ID
                        .name(user.getFirstName() + " " + user.getLastName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .department("") // Add if you have department field
                        .status(user.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
}