package com.resumeai.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** AI provider integration used by generation workflows. */
@Component
public class AiProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(AiProviderFactory.class);

    private final List<AiProvider> providers;

    @Value("${ai.provider:auto}")
    private String preferredProvider;

    public AiProviderFactory(GeminiProvider gemini, NvidiaProvider nvidia) {
        this.providers = List.of(gemini, nvidia);
    }

    /**
     * Returns the best available provider.
     * If a specific provider is configured via ai.provider property, use that.
     * Otherwise auto-detect based on availability.
     */
    public AiProvider getProvider() {
        // If user explicitly configured a provider
        if (!"auto".equalsIgnoreCase(preferredProvider)) {
            for (AiProvider p : providers) {
                if (p.getModelName().toLowerCase().contains(preferredProvider.toLowerCase()) && p.isAvailable()) {
                    log.debug("[AiProviderFactory] Using configured provider: {}", p.getModelName());
                    return p;
                }
            }
            log.warn("[AiProviderFactory] Configured provider '{}' not available, falling back to auto", preferredProvider);
        }

        // Auto-detect
        for (AiProvider p : providers) {
            if (p.isAvailable()) {
                log.debug("[AiProviderFactory] Auto-selected provider: {}", p.getModelName());
                return p;
            }
        }

        throw new AiProviderException(
            "No AI provider available. Set GEMINI_API_KEY or NVIDIA_API_KEY environment variable."
        );
    }

    /**
     * Generates text using the best available provider.
     * If the primary provider fails, falls back to the next available one.
     */
    public AiProviderResult generateWithFallback(String prompt) {
        if (!"auto".equalsIgnoreCase(preferredProvider)) {
            AiProvider provider = getProvider();
            try {
                log.debug("[AiProviderFactory] Using configured provider without cross-provider fallback: {}", provider.getModelName());
                String result = provider.generate(prompt);
                return new AiProviderResult(result, provider.getModelName());
            } catch (Exception ex) {
                throw new AiProviderException("Configured provider failed: " + provider.getModelName(), ex);
            }
        }

        List<AiProvider> orderedProviders = new ArrayList<>();
        AiProvider preferred = null;

        try {
            preferred = getProvider();
            orderedProviders.add(preferred);
        } catch (Exception ignored) {
            // Keep trying all available providers below.
        }

        for (AiProvider provider : providers) {
            if (provider.isAvailable() && provider != preferred) {
                orderedProviders.add(provider);
            }
        }

        for (int index = 0; index < orderedProviders.size(); index++) {
            AiProvider provider = orderedProviders.get(index);
            try {
                if (index == 0) {
                    log.debug("[AiProviderFactory] Trying primary provider: {}", provider.getModelName());
                } else {
                    log.info("[AiProviderFactory] Trying fallback provider: {}", provider.getModelName());
                }
                String result = provider.generate(prompt);
                return new AiProviderResult(result, provider.getModelName());
            } catch (Exception ex) {
                if (index == 0) {
                    log.warn("[AiProviderFactory] Primary provider {} failed: {}", provider.getModelName(), ex.getMessage());
                } else {
                    log.warn("[AiProviderFactory] Fallback provider {} also failed: {}", provider.getModelName(), ex.getMessage());
                }
            }
        }

        throw new AiProviderException("All AI providers failed");
    }

    /**
     * Result wrapper that includes which model was used.
     */
    public record AiProviderResult(String text, String model) {}
}
