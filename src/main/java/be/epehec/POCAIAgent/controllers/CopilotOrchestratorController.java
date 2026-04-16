package be.epehec.POCAIAgent.controllers;

import be.epehec.POCAIAgent.services.CopilotOrchestrator;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/copilot")
public class CopilotOrchestratorController {

    private CopilotOrchestrator orchestrator;

    /**
     * Endpoint to upload a document and get an AI security analysis.
     * Supports PDF, Word, and Text files via Apache Tika.
     */
    @PostMapping("/analyze")
    public ResponseEntity<String> uploadAndAnalyze(@RequestParam("file") MultipartFile file) {
        try {
            // 1. Initialize Tika to extract text from the uploaded file
            Tika tika = new Tika();
            String documentText = tika.parseToString(file.getInputStream());

            // 2. Process the text through the Semantic Kernel Orchestrator
            String verdictReport = orchestrator.analyzeDocument(documentText);

            // 3. Return the AI-generated synthesis report
            return ResponseEntity.ok(verdictReport);

        } catch (IOException e) {
            // Handle file reading errors
            return ResponseEntity.status(400).body("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            // Handle AI processing or framework errors
            return ResponseEntity.status(500).body("Analysis failed: " + e.getMessage());
        }
    }
}
