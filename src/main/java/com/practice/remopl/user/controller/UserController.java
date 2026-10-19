package com.practice.remopl.user.controller;

import com.practice.remopl.user.dto.request.UserCreateRequest;
import com.practice.remopl.user.dto.resposne.UserResponse;
import com.practice.remopl.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody UserCreateRequest userCreateRequest
    ) {
        UserResponse response = userService.create(userCreateRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
