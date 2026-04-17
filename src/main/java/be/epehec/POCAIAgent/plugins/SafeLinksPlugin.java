package be.epehec.POCAIAgent.plugins;

import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Safe Links Security Agent Plugin.
 * <p>
 * Scans document text for URLs and evaluates their safety.
 * Malicious or suspicious links are blocked to protect users.
 * </p>
 * <p>
 * <b>Workshop Note:</b> This PoC uses a local blacklist simulation.
 * In production, this would integrate directly with Microsoft Defender
 * Threat Intelligence APIs to evaluate URLs in real-time.
 * </p>
 */
@Slf4j
@Component
public class SafeLinksPlugin {

    // Simulated Microsoft Defender Blacklist for the PoC
    private static final List<String> MALICIOUS_DOMAINS = List.of(
            "phishing.com",
            "malware-download.net",
            "freedumps.org",
            "hack-ephec.xyz"
    );

    /**
     * Extracts URLs from the text and replaces malicious ones with a security warning.
     *
     * @param text The raw document text to scan for links.
     * @return The text with malicious URLs blocked.
     */
    @DefineKernelFunction(
            name = "scanAndBlockUrls",
            description = "Scans text for URLs and blocks known malicious links."
    )
    public String scanAndBlockUrls(
            @KernelFunctionParameter(
                    name = "text",
                    description = "The text to scan for URLs"
            ) String text
    ) {
        // Regex to find standard HTTP/HTTPS URLs
        String urlRegex = "https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_+.~#?&/=]*)";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(text);

        String processedText = text;

        // Iterate through all found URLs in the document
        while (matcher.find()) {
            String foundUrl = matcher.group();

            // Check if the URL contains any of our blacklisted domains
            boolean isMalicious = MALICIOUS_DOMAINS.stream().anyMatch(foundUrl::contains);

            if (isMalicious) {
                log.info("Defender Alert: Blocked malicious link -> " + foundUrl);
                processedText = processedText.replace(foundUrl, "[BLOCKED BY DEFENDER SAFE LINKS]");
            }
        }

        return processedText;
    }
}
