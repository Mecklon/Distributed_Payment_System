package com.mecklon.auth.controllers;


import com.mecklon.core.dtos.AuthRequest;
import com.mecklon.core.dtos.AuthResponse;
import com.mecklon.core.dtos.SignupRequest;
import com.mecklon.auth.exceptions.UserAlreadyExistsException;
import com.mecklon.auth.models.User;
import com.mecklon.auth.repositories.UserRepository;
import com.mecklon.auth.services.CustomUserDetails;
import com.mecklon.auth.services.CustomUserDetailsService;
import com.mecklon.core.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {

        System.out.println(request.getEmail());
        System.out.println(request.getPassword());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService
                .loadUserByUsername(request.getEmail());



        String token = jwtUtil.generateToken(userDetails.getUsername(), userDetails.getId(), userDetails.getDisplayUsername());
        User user = userRepository.findByEmail(userDetails.getUsername());
        return new AuthResponse(user.getId(),token, userDetails.getUsername(), userDetails.getDisplayUsername(), user.getAddress() );
    }


    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {

        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {

            throw new BadCredentialsException("Username, email or password is missing");
        }

        User existingUser = userRepository.findByEmail(request.getEmail());

        if (existingUser != null) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .address(request.getAddress())
                .build();

        user = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        String token = jwtUtil.generateToken(userDetails.getUsername(), userDetails.getId(), userDetails.getDisplayUsername());

        AuthResponse response = new AuthResponse(
                user.getId(),
                token,
                user.getEmail(),
                user.getUsername(),
                user.getAddress()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
