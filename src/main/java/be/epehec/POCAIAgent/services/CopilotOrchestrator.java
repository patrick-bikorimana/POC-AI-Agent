package be.epehec.POCAIAgent.services;

import be.epehec.POCAIAgent.plugins.AcademicIntegrityPlugin;
import be.epehec.POCAIAgent.plugins.DlpPlugin;
import be.epehec.POCAIAgent.plugins.SafeLinksPlugin;
import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.OpenAIServiceVersion;
import com.azure.core.credential.AzureKeyCredential;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import jakarta.annotation.PostConstruct;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class CopilotOrchestrator {
    @Value("${azure.openai.endpoint}")
    private String endpoint;

    @Value("${azure.openai.key}")
    private String apiKey;

    @Value("${azure.openai.deployment-name}")
    private String deploymentName;

    private Kernel kernel;

    private final AcademicIntegrityPlugin integrityPlugin;
    private final DlpPlugin dlpPlugin;
    private final SafeLinksPlugin safeLinksPlugin;

    @PostConstruct
    public void init() {
        OpenAIAsyncClient client = new OpenAIClientBuilder()
                .credential(new AzureKeyCredential(apiKey))
                .endpoint(endpoint)
                .serviceVersion(OpenAIServiceVersion.V2025_01_01_PREVIEW)
                .buildAsyncClient();

        ChatCompletionService chat = OpenAIChatCompletion.builder()
                .withOpenAIAsyncClient(client)
                .withModelId(deploymentName)
                .build();

        // Register plugins
        KernelPlugin dlpAgent = KernelPluginFactory.createFromObject(dlpPlugin, "DlpAgent");
        KernelPlugin integrityAgent = KernelPluginFactory.createFromObject(integrityPlugin, "IntegrityAgent");
        KernelPlugin safeLinksAgent = KernelPluginFactory.createFromObject(safeLinksPlugin, "SafeLinksAgent");

        this.kernel = Kernel.builder()
                .withAIService(ChatCompletionService.class, chat)
                .withPlugin(dlpAgent)
                .withPlugin(integrityAgent)
                .withPlugin(safeLinksAgent)
                .build();
    }

    /**
     * Splits a large text into smaller chunks of a specific size.
     */
    private List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
        return chunks;
    }

    /**
     * Main entry point for document analysis.
     * Uses CompletableFuture to process chunks in parallel.
     */
    public String analyzeDocument(String documentText) {
        log.info("Beginning of the concurrent process...");
        long startTime = System.currentTimeMillis();

        List<String> chunks = splitIntoChunks(documentText, 2000);
        log.info("Document split into {} chunk(s).", chunks.size());

        List<CompletableFuture<String>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() -> processSingleChunk(chunk)))
                .toList();

        String aggregatedResults;
        try {
            // 3. GATHER : Wait for all threads to finish
            aggregatedResults = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.joining("\n\n--- NEXT CHUNK ---\n\n"));

        } catch (java.util.concurrent.CompletionException e) {
            // UNBOXING THE EXCEPTION: Check if the background thread threw our Security Exception
            Throwable cause = e.getCause();
            if (cause instanceof be.epehec.POCAIAgent.exceptions.PromptInjectionException) {
                // We extract it and throw it to the GlobalExceptionHandler
                throw (be.epehec.POCAIAgent.exceptions.PromptInjectionException) cause;
            }
            throw e; // If it's a normal error, throw it as is
        }

        long endTime = System.currentTimeMillis();
        log.info("Concurrent process ended in {} ms.", endTime - startTime);

        return generateFinalSynthesis(aggregatedResults);
    }

    /**
     * Executes the security agents on a single chunk.
     */
    private String processSingleChunk(String chunk) {
        String prompt = """
        You are an AI Security Worker handling a specific chunk of a document.
        Follow this strict workflow:
        1. Call SafeLinksAgent.scanAndBlockUrls.
        2. Call DlpAgent.redactData.
        3. Call IntegrityAgent.calculateIntegrityScore.
        
        Output the processed chunk and the isolated integrity score.
        
        Chunk Data: {{$chunk}}
        """;

        KernelFunctionArguments arguments = KernelFunctionArguments.builder()
                .withVariable("chunk", chunk)
                .build();

        try {
            FunctionResult<Object> result = kernel.invokePromptAsync(prompt, arguments).block();
            return java.util.Objects.requireNonNull(result.getResult()).toString();

        } catch (Exception e) {
            // Check if Azure's Guardrail blocked this specific chunk
            if (e.getMessage() != null && e.getMessage().contains("content_filter")) {
                throw new be.epehec.POCAIAgent.exceptions.PromptInjectionException("Jailbreak of Azure OpenAI Guardrail blocked.");
            }
            // Wrap any other standard exception into a RuntimeException so CompletableFuture can handle it
            throw new RuntimeException(e);
        }
    }

    /**
     * The Aggregator Agent creates the final report based on all processed chunks.
     */
    private String generateFinalSynthesis(String aggregatedData) {
        String synthesisPrompt = """
        You are the Head Aggregator Agent.
        You have received data processed by parallel worker agents. 
        Synthesize the findings into a unified 'Synthesized Professor Report & Verdict'.
        Do not re-process the data, just summarize the redactions, blocked links, and average integrity score.
        
        Aggregated Worker Data:
        {{$data}}
        """;

        KernelFunctionArguments arguments = KernelFunctionArguments.builder()
                .withVariable("data", aggregatedData)
                .build();

        FunctionResult<Object> result = kernel.invokePromptAsync(synthesisPrompt, arguments).block();
        return Objects.requireNonNull(result.getResult()).toString();
    }
}
