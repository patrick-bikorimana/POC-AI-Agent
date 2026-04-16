package be.epehec.POCAIAgent.services;

import be.epehec.POCAIAgent.plugins.AcademicIntegrityPlugin;
import be.epehec.POCAIAgent.plugins.DlpPlugin;
import be.epehec.POCAIAgent.plugins.SafeLinksPlugin;
import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

@RequiredArgsConstructor
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

    public String analyzeDocument(String documentText) {
        // We update the prompt to tell the LLM to use both tools sequentially
        String prompt = """
        You are the Academic Compliance & Security Copilot. 
        Follow this strict workflow:
        1. Call DlpAgent.redactData to sanitize the document.
        2. Call IntegrityAgent.calculateIntegrityScore on the SANITIZED text.
        3. Create a final 'Synthesized Professor Report' based on the findings.
        
        Document: {{$doc}}
        """;

        KernelFunctionArguments arguments = KernelFunctionArguments.builder()
                .withVariable("doc", documentText)
                .build();

        FunctionResult<Object> result = kernel.invokePromptAsync(prompt, arguments).block();
        return Objects.requireNonNull(result.getResult(), "AI answer is null").toString();
    }
}
