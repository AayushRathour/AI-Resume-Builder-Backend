package com.resumeai.ai.util;

/**
 * Centralised prompt builder — constructs all server-side prompts
 * and sanitizes user input to prevent prompt injection.
 */
public final class PromptBuilder {

    private PromptBuilder() {}

    public static String buildSummaryPrompt(String jobTitle, String yearsOfExperience,
                                            String keySkills, String additionalContext) {
        return String.format(
            "You are a professional resume writer. Generate a concise, impactful professional " +
            "resume summary (3-4 sentences) for the following profile:\n\n" +
            "Job Title: %s\n" +
            "Years of Experience: %s\n" +
            "Key Skills: %s\n" +
            "Additional Context: %s\n\n" +
            "Write in first person. Focus on value delivered, not just duties. " +
            "Output only the summary text — no labels or explanations.",
            sanitize(jobTitle), sanitize(yearsOfExperience),
            sanitize(keySkills), sanitize(additionalContext)
        );
    }

    public static String buildBulletsPrompt(String jobTitle, String companyName,
                                            String responsibilities, String achievements) {
        return String.format(
            "You are a professional resume writer. Generate 5 strong, ATS-optimised resume " +
            "bullet points for the following job experience:\n\n" +
            "Job Title: %s\n" +
            "Company: %s\n" +
            "Responsibilities: %s\n" +
            "Achievements: %s\n\n" +
            "Each bullet must start with a strong action verb and include quantifiable results " +
            "where possible. Output only the bullet list — no explanations.",
            sanitize(jobTitle), sanitize(companyName),
            sanitize(responsibilities), sanitize(achievements)
        );
    }

    public static String buildCoverLetterPrompt(String jobTitle, String companyName,
                                                String jobDescription, String applicantSummary) {
        return String.format(
            "You are a professional resume writer. Write a compelling cover letter for:\n\n" +
            "Position: %s at %s\n\n" +
            "Job Description:\n%s\n\n" +
            "Applicant Profile:\n%s\n\n" +
            "The letter should be professional, tailored to the role, and no longer than 4 paragraphs. " +
            "Output only the cover letter text.",
            sanitize(jobTitle), sanitize(companyName),
            sanitize(jobDescription), sanitize(applicantSummary)
        );
    }

    public static String buildImprovePrompt(String sectionType, String currentContent, String targetRole) {
        return String.format(
            "You are a professional resume writer. Improve the following resume %s section " +
            "to be more impactful for a %s role:\n\n" +
            "Current Content:\n%s\n\n" +
            "Make it more specific, achievement-oriented, and ATS-friendly. " +
            "Output only the improved content — no explanations.",
            sanitize(sectionType), sanitize(targetRole), sanitize(currentContent)
        );
    }

    public static String buildAtsPrompt(String resumeContent, String jobDescription) {
        return String.format(
            "You are an ATS (Applicant Tracking System) expert. Analyse this resume against " +
            "the job description and respond ONLY in this exact JSON format:\n" +
            "{\n" +
            "  \"score\": <integer 0-100>,\n" +
            "  \"missingKeywords\": [\"keyword1\", \"keyword2\", ...],\n" +
            "  \"recommendations\": \"<concise actionable recommendations>\"\n" +
            "}\n\n" +
            "Resume:\n%s\n\n" +
            "Job Description:\n%s",
            sanitize(resumeContent), sanitize(jobDescription)
        );
    }

    public static String buildSkillsPrompt(String jobTitle, String currentSkills, String industry) {
        return String.format(
            "You are a career advisor. Suggest 10 in-demand skills for a %s in the %s industry.\n\n" +
            "The candidate already has: %s\n\n" +
            "Focus on skills they are MISSING. Categorise into: Technical Skills, Soft Skills, " +
            "and Tools/Platforms. Output as a structured list.",
            sanitize(jobTitle), sanitize(industry), sanitize(currentSkills)
        );
    }

    public static String buildTailorPrompt(String resumeContent, String jobDescription, String jobTitle) {
        return String.format(
            "You are a professional resume writer. Rewrite the following resume to be perfectly " +
            "tailored for the %s position described below.\n\n" +
            "Job Description:\n%s\n\n" +
            "Original Resume:\n%s\n\n" +
            "Preserve all factual information. Adjust phrasing, keywords, and emphasis to match " +
            "the job requirements. Output only the full rewritten resume.",
            sanitize(jobTitle), sanitize(jobDescription), sanitize(resumeContent)
        );
    }

    public static String buildTranslatePrompt(String resumeContent, String targetLanguage) {
        return String.format(
            "Translate the following professional resume into %s. " +
            "Maintain professional tone, proper formatting, and industry-specific terminology. " +
            "Output only the translated resume.\n\n" +
            "Resume:\n%s",
            sanitize(targetLanguage), sanitize(resumeContent)
        );
    }

    /**
     * Sanitizes input to prevent prompt injection attacks.
     * Strips control characters and trims excessive whitespace.
     */
    public static String sanitize(String input) {
        if (input == null) return "";
        // Remove any attempts to inject new instructions
        return input
                .replaceAll("(?i)(ignore|disregard|forget).{0,30}(previous|above|all).{0,30}(instruction|prompt|context)", "[FILTERED]")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "") // remove control chars
                .trim();
    }
}
