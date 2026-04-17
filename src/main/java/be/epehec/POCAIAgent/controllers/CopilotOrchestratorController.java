package be.epehec.POCAIAgent.controllers;

import be.epehec.POCAIAgent.services.CopilotOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/copilot")
public class CopilotOrchestratorController {

    private final CopilotOrchestrator orchestrator;

    /**
     * Endpoint to upload a document and get an AI security analysis.
     * Supports PDF, Word, and Text files via Apache Tika.
     */
    @PostMapping("/analyze")
    public ResponseEntity<String> uploadAndAnalyze(@RequestParam("file") MultipartFile file) throws Exception{
        // 1. Read the file
        Tika tika = new Tika();
        String documentText = tika.parseToString(file.getInputStream());

        // 2. Validate content
        if (documentText == null || documentText.trim().isEmpty()) {
            throw new IllegalArgumentException("Tika found no text in this file. Please ensure it is a valid PDF or Word document.");
        }

        // 3. Process with AI Orchestrator
        String verdictReport = orchestrator.analyzeDocument(documentText);

        // 4. Return success response
        return ResponseEntity.ok(verdictReport);
    }
}
