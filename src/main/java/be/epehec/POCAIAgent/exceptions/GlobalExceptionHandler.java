package be.epehec.POCAIAgent.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the Copilot API.
 * Intercepts exceptions thrown by any controller and formats a clean HTTP response.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles security exceptions when a Prompt Injection (Jailbreak) is detected.
     * Returns a 403 Forbidden status code.
     */
    @ExceptionHandler(PromptInjectionException.class)
    public ResponseEntity<String> handlePromptInjection(PromptInjectionException ex) {
        log.info("SECURITY ALERT CAUGHT BY ADVICE: " + ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("SECURITY ALERT: Document blocked. " + ex.getMessage());
    }

    /**
     * Handles bad requests, for example when a file is empty or unreadable by Tika.
     * Returns a 400 Bad Request status code.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("⚠️ Bad Request: " + ex.getMessage());
    }

    /**
     * Fallback handler for all other unexpected exceptions.
     * Returns a 500 Internal Server Error status code.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralExceptions(Exception ex) {
        log.info("🔥 UNEXPECTED ERROR CAUGHT:");
        ex.printStackTrace(); // Log the full stack trace for debugging

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Analysis failed due to an internal error: " + ex.getMessage());
    }
}
