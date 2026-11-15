package com.ben.messagingplatform.user.service;


import com.ben.messagingplatform.infrastructure.rabbitmq.security.JwtService;
import com.ben.messagingplatform.user.dto.LoginRequest;
import com.ben.messagingplatform.user.dto.LoginResponse;
import com.ben.messagingplatform.user.entity.User;
import com.ben.messagingplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());

        return new LoginResponse(accessToken, "Bearer");
    }
}
