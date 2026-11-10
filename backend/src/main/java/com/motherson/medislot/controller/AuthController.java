package com.motherson.medislot.controller;

import com.motherson.medislot.dto.Dtos.LoginRequest;
import com.motherson.medislot.dto.Dtos.LoginResponse;
import com.motherson.medislot.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }
}
