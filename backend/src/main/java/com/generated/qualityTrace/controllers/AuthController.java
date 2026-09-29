package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.constants.RoutePaths;
import com.generated.qualityTrace.services.AuthService;
import com.generated.qualityTrace.types.LoginPayload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutePaths.AUTH)
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public Map<String, Object> login(@Valid @RequestBody LoginPayload payload,
                                   HttpServletRequest request) {
    return authService.login(
        payload.username(),
        payload.password(),
        request.getRequestURI()
    );
  }
}
