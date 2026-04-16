package be.epehec.POCAIAgent.plugins;

import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
import org.springframework.stereotype.Component;
/**
 * Data Loss Prevention (DLP) Plugin for Microsoft Semantic Kernel.
 * <p>
 * This component acts as a specialized tool within the Academic Compliance & Security Copilot.
 * It is automatically invoked by the Orchestrator's Large Language Model (LLM) when the model
 * determines that sensitive information must be redacted before generating a final report.
 * </p>
 * <p>
 * <b>Workshop Note:</b> This is a Proof of Concept (PoC) relying on standard Regex.
 * In a production enterprise scenario, this agent would interface with services like
 * Azure AI Language PII detection or Microsoft Purview.
 * </p>
 */
@Component
public class DlpPlugin {
    @DefineKernelFunction(
            name = "redactData",
            description = "Scans text for PII (names, emails) or API keys and replaces them with [REDACTED]."
    )
    public String redactData(
            @KernelFunctionParameter(
                    name = "documentText",
                    description = "The document text to censor"
            ) String documentText
    ) {
        // Real-world: Use regex, Azure AI Language (PII detection), or rules here.
        // PoC level: Simple string replacement for demonstration.
        String censored = documentText.replaceAll("\\S+@\\S+\\.\\S+", "[REDACTED EMAIL]");
        censored = censored.replaceAll("sk-[a-zA-Z0-9]{32,}", "[REDACTED API KEY]");

        // Belgians phone numbers
        censored = censored.replaceAll("(?:\\+32|0)[1-9][0-9]{2}[ .-]?[0-9]{2}[ .-]?[0-9]{2}[ .-]?[0-9]{2}", "[REDACTED PHONE]");

        // 3. Credit cards
        censored = censored.replaceAll("\\b(?:\\d[ -]*?){13,16}\\b", "[REDACTED CREDIT CARD]");

        // 4. Belgians banks accounts
        censored = censored.replaceAll("BE\\d{2}\\s?\\d{4}\\s?\\d{4}\\s?\\d{4}", "[REDACTED IBAN]");

        System.out.println("Shield active: Redacted document data.");
        return censored;
    }
}