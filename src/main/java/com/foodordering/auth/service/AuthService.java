package com.foodordering.auth.service;

import com.foodordering.auth.dto.AuthResponse;
import com.foodordering.auth.dto.LoginRequest;
import com.foodordering.auth.dto.RegisterRequest;
import com.foodordering.auth.entity.User;
import com.foodordering.auth.repository.UserRepository;
import com.foodordering.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://localhost:8080/api/v1/users/register}")
    private String userServiceRegisterUrl;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole());

        Set<String> roles = new HashSet<>();
        switch (request.getRole()) {
            case "CUSTOMER":
                roles.add("USER");
                break;
            case "RESTAURANT":
                roles.add("RESTAURANT");
                break;
            case "ADMIN":
                roles.add("ADMIN");
                break;
        }
        user.setRoles(roles);

        userRepository.save(user);

        // --- Inter-service call to user-service ---
        try {
            Map<String, Object> userPayload = new HashMap<>();
            userPayload.put("email", request.getEmail());
            userPayload.put("password", request.getPassword()); // Send raw password, user-service will hash
            userPayload.put("fullName", request.getFullName());
            userPayload.put("phoneNumber", request.getPhoneNumber());
            userPayload.put("role", request.getRole());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(userPayload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(userServiceRegisterUrl, entity, String.class);
            System.out.println("[INFO] user-service registration response: " + response.getStatusCode() + " - " + response.getBody());
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("User-service registration failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not register user in user-service: " + e.getMessage(), e);
        }
        // --- End inter-service call ---

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        return new AuthResponse(jwt, user.getEmail(), user.getFullName(), user.getRoles(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new AuthResponse(jwt, user.getEmail(), user.getFullName(), user.getRoles(), user.getRole());
    }
} 