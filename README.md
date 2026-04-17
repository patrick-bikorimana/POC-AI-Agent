# Academic Compliance & Security Copilot (PoC)

## 📌 Overview
The **Academic Compliance & Security Copilot** is a Proof of Concept (PoC) demonstrating a Multi-Agent orchestration system built with **Java Spring Boot 3** and **Microsoft Semantic Kernel (v1.4.0)**.

Designed for the education sector, this intelligent backend analyzes student document submissions (PDF/Word) to detect academic misconduct, redact sensitive data (DLP), block malicious links, and prevent LLM manipulation (Prompt Injections).

![diagram-ai-agent-local.png](media/diagram-ai-agent-local.png)

## 🏗 Architecture & Workflow
This application relies on an **Agentic AI Design Pattern** combined with a **Scatter-Gather** multithreading approach. The workflow is as follows:

1. **Document Ingestion:** The user uploads a file (`.pdf`, `.docx`, `.txt`). **Apache Tika** extracts the raw text.
2. **Anti-Prompt Injection Guardrail:** Before processing, the request is validated by Azure AI's native Content Safety filters. If a jailbreak attempt is detected, the process halts immediately, throwing a custom `PromptInjectionException`.
3. **Scatter (Chunking):** The Core Copilot orchestrator splits large documents into smaller chunks to respect LLM Token limits.
4. **Parallel Processing (Specialized Agents):** Using Java `CompletableFuture`, chunks are processed asynchronously. For each chunk, the following agents run simultaneously via Function Calling:
   * 🛡️ **DLP & Data Privacy Agent:** Uses Regex rules to find and mask Personally Identifiable Information (PII) and exposed API keys (`[REDACTED]`).
   * 🔗 **Safe Links Agent:** Simulates Microsoft Defender Threat Intelligence by scanning for and blocking known malicious URLs.
   * 🎓 **Academic Integrity Agent:** Analyzes the document's structure and syntax to calculate an AI-generation probability score.
5. **Gather (Synthesis):** The orchestrator waits for all threads to finish, compiles the cleaned chunks, and prompts the Aggregator LLM to generate a synthesized "Professor Report & Verdict."

## 🚀 Advanced Engineering Features

During the development of this PoC, several enterprise-level challenges were addressed:

* **Parallelization & Chunking:** Processing a large PDF sequentially causes severe HTTP timeouts and exceeds Azure OpenAI's context window. We implemented the Scatter-Gather pattern. The document is chunked and processed in parallel across multiple background threads, drastically reducing processing time and guaranteeing we never hit token limits.
* **Async Exception Unboxing:** To keep Controllers clean, we implemented Aspect-Oriented Programming (AOP) using `@RestControllerAdvice`. When Azure AI Content Safety blocks a Prompt Injection during parallel processing, Java wraps the error inside a generic `CompletionException`. We explicitly "unbox" this exception during the Gather phase to extract the root cause and throw our custom `PromptInjectionException`, ensuring the API gracefully returns a strict `403 Forbidden` security alert.
## 🛡️ Security & Exception Handling

A core feature of this architecture is its robust and clean approach to error handling, specifically designed to catch AI manipulation attempts (Jailbreaks) even in a multithreaded environment.

### 1. Custom Security Exceptions
We created a dedicated `PromptInjectionException` to cleanly separate business/security logic from standard technical errors. When the Azure Guardrail detects an attack (like *"Disregard all previous instructions"*), the Orchestrator throws this specific exception instead of a generic server crash.

### 2. Global Exception Handler (AOP)
To maintain the **Separation of Concerns** principle, the API Controllers are completely stripped of `try/catch` blocks. Instead, we use **Aspect-Oriented Programming (AOP)** via Spring's `@RestControllerAdvice`.
The `GlobalExceptionHandler` acts as a centralized control tower:
* It catches `PromptInjectionException` and automatically formats a clean `403 Forbidden` HTTP response.
* It catches `IllegalArgumentException` (e.g., empty files) and returns a `400 Bad Request`.
* It catches unexpected errors and returns a safe `500 Internal Server Error` without leaking stack traces to the end-user.

### 3. The "Unboxing" Challenge in Parallel Processing
When switching to parallel processing with `CompletableFuture`, background threads do not throw exceptions directly to the main thread. Instead, Java wraps them in a `CompletionException`.
If left untreated, our `@RestControllerAdvice` would fail to recognize the security alert. To solve this, our Gather phase implements an **Exception Unboxing** mechanism: it catches the `CompletionException`, extracts the root `PromptInjectionException` from the background thread, and safely re-throws it to the Global Handler.
## 📋 Prerequisites
* **Java 21** or higher
* **Maven**
* **Azure Account** with access to Azure OpenAI Service
* An active Azure OpenAI Deployment (e.g., `gpt-4o-mini` or `gpt-4o`) supporting API version `2025-01-01-preview` or later.

## ⚙️ Configuration & Setup

1. **Clone the repository:**
   ```bash
   git clone <your-repository-url>
   cd POCAIAgent