package pl.lukbol.dyplom.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.DTOs.notification.NotificationRequestDTO;
import pl.lukbol.dyplom.services.NotificationService;



import static pl.lukbol.dyplom.utilities.AuthenticationUtils.checkmail;

@RestController
@RequiredArgsConstructor
public class NotificationController {


    private final NotificationService notificationService;

    @DeleteMapping(value = "/removeAlerts", consumes = {"*/*"})
    public ResponseEntity<ApiResponseDTO> removeAlerts(Authentication authentication) {

        ApiResponseDTO response = notificationService.removeAlerts(authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-notification")
    public ResponseEntity<ApiResponseDTO> createNotification(
            Authentication authentication,
            @RequestBody NotificationRequestDTO notificationRequestDTO) {

        String userEmail = checkmail(authentication.getPrincipal());
        ApiResponseDTO response = notificationService.createNotification(userEmail, notificationRequestDTO);

        return ResponseEntity.ok(response);

    }
}
