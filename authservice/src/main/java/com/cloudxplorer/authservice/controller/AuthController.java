package com.cloudxplorer.authservice.controller;

import com.cloudxplorer.authservice.model.*;
import com.cloudxplorer.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestBody User request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, correlationId));
    }

    @GetMapping("/login/authorize")
    public ResponseEntity<?> authorizeUrl(
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam(value = "redirectUri", required = false) String redirectUri,
            @RequestParam(value = "scope", required = false) String scope,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.buildAuthorizeUrl(redirectUri, scope));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.profile(jwt));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestBody ProfileUpdateRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.updateProfile(jwt, request));
    }

    @DeleteMapping("/profile")
    public ResponseEntity<?> deleteProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.deleteProfile(jwt));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.logout(jwt, request));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<?> adminGetUserByEmail(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam("email") String email,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.adminGetUser(jwt, email));
    }

    @PostMapping("/admin/users")
    public ResponseEntity<?> adminCreateUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestBody AdminUserRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.adminCreateUser(jwt, request));
    }

    @PutMapping("/admin/users")
    public ResponseEntity<?> adminUpdateUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam("email") String email,
            @RequestBody AdminUserUpdateRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.adminUpdateUser(jwt, email, request));
    }

    @DeleteMapping("/admin/users")
    public ResponseEntity<?> adminDeleteUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam("email") String email,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.adminDeleteUser(jwt, email));
    }

    @PutMapping("/support/users")
    public ResponseEntity<?> supportUpdateUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam("email") String email,
            @RequestBody ProfileUpdateRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.supportUpdateUser(jwt, email, request));
    }

    @GetMapping("/airline/users")
    public ResponseEntity<?> airlineViewUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam("email") String email,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.airlineViewUser(jwt, email));
    }
}
