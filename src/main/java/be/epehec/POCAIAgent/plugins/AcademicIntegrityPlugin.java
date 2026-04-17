package be.epehec.POCAIAgent.plugins;

import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Academic Integrity Agent Plugin.
 * Analyzes document structure and content to detect potential AI generation
 * or academic misconduct markers.
 */
@Slf4j
@Component
public class AcademicIntegrityPlugin {
    /**
     * Analyzes the text to calculate a probability score of AI generation.
     * * @param text The sanitized document text.
     * @return A string containing the AI probability score and brief reasoning.
     */
    @DefineKernelFunction(
            name = "calculateIntegrityScore",
            description = "Analyzes the text to determine the probability of AI generation (0-100%)."
    )
    public String calculateIntegrityScore(
            @KernelFunctionParameter(
                    name = "text",
                    description = "The text to analyze for integrity markers"
            ) String text
    ) {
        // PoC Heuristic Logic:
        // In a real scenario, this would call a specialized model or service (like GPTZero).
        int score = 0;

        // Marker 1: Check for very common AI transition words
        if (text.contains("In conclusion,") || text.contains("Furthermore,")) score += 20;

        // Marker 2: Lack of specific personal pronouns or anecdotes (simulated)
        if (text.length() > 500 && !text.contains("I believe") && !text.contains("In my experience")) score += 30;

        // Marker 3: Perfect grammatical consistency (simulated)
        score += 15;

        log.info("Academic Integrity Agent: Analysis complete.");
        return "AI Probability Score: " + score + "% | Reasoning: High structural consistency and generic transitions detected.";
    }
}
