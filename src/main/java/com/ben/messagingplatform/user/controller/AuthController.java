package com.ben.messagingplatform.user.controller;

import com.ben.messagingplatform.user.dto.LoginRequest;
import com.ben.messagingplatform.user.dto.LoginResponse;
import com.ben.messagingplatform.user.dto.RegisterRequest;
import com.ben.messagingplatform.user.service.AuthService;
import com.ben.messagingplatform.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

        @PostMapping("/login")
        public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
            LoginResponse response = authService.login(request);

            return ResponseEntity.ok(response);
        }
    }

