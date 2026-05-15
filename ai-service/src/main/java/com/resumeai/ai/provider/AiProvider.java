package com.resumeai.ai.provider;

/** Abstraction for pluggable AI provider integrations. */
public interface AiProvider {

    /**
     * Sends a prompt to the AI model and returns the raw text response.
     *
     * @param prompt  the user prompt
     * @return the AI-generated text content (never null, may be empty on failure)
     * @throws AiProviderException if the provider call fails after retries
     */
    String generate(String prompt);

    /**
     * @return human-readable name of the provider (e.g. "gemini-1.5-flash")
     */
    String getModelName();

    /**
     * @return true if this provider is configured and available
     */
    boolean isAvailable();
}
