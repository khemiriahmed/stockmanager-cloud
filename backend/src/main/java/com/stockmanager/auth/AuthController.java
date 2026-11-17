package com.stockmanager.auth;

import com.stockmanager.auth.dto.LoginRequest;
import com.stockmanager.auth.dto.LoginResponse;
import com.stockmanager.security.JwtService;
import com.stockmanager.user.User;
import com.stockmanager.user.UserRepository;
import com.stockmanager.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService,
                          JwtService jwtService,
                          UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion et génération d'un jeton JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email().toLowerCase());
        String token = jwtService.generateToken(userDetails);
        User user = userRepository.findByEmailIgnoreCase(userDetails.getUsername()).orElseThrow();
        return new LoginResponse(token, "Bearer", jwtService.expirationSeconds(), UserDto.from(user));
    }
}
