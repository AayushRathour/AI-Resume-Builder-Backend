package com.resumeai.ai.util;

/**
 * Centralised prompt builder  constructs all server-side prompts
 * and sanitizes user input to prevent prompt injection.
 */
public final class PromptBuilder {

    private PromptBuilder() {}

    public static String buildSummaryPrompt(String jobTitle, String yearsOfExperience,
                                            String keySkills, String additionalContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional resume writer. Generate a concise, impactful professional resume summary (3-4 sentences) for the following candidate profile:\n\n");
        prompt.append("Job Title: ").append(sanitize(jobTitle)).append("\n");
        prompt.append("Years of Experience: ").append(sanitize(yearsOfExperience)).append("\n");
        prompt.append("Key Skills: ").append(sanitize(keySkills)).append("\n");
        
        if (additionalContext != null && !additionalContext.isBlank()) {
            prompt.append("Mandatory Context/Specific Requirements: ").append(sanitize(additionalContext)).append("\n");
            prompt.append("\nCRITICAL: You MUST strictly incorporate the 'Mandatory Context' provided above. If it contains specific roles, achievements, or a draft summary, refine and expand upon it while maintaining its core message.\n");
        }

        prompt.append("\nGuidelines:\n");
        prompt.append("- Write in a confident, first-person professional tone.\n");
        prompt.append("- Focus on value delivered and measurable impact, not just duties.\n");
        prompt.append("- Optimize for ATS by including relevant keywords from the profile.\n");
        prompt.append("- Output ONLY the summary text  no labels, no greetings, and no explanations.");
        
        return prompt.toString();
    }

    public static String buildBulletsPrompt(String jobTitle, String companyName,
                                            String responsibilities, String achievements) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional resume writer. Generate 5 strong, high-impact resume bullet points for the following work experience:\n\n");
        prompt.append("Job Title: ").append(sanitize(jobTitle)).append("\n");
        prompt.append("Company: ").append(sanitize(companyName)).append("\n");
        prompt.append("Responsibilities: ").append(sanitize(responsibilities)).append("\n");
        prompt.append("Achievements: ").append(sanitize(achievements)).append("\n\n");
        
        prompt.append("Guidelines for Bullets:\n");
        prompt.append("- Use the 'Action Verb + Task + Result' (Google XYZ) formula.\n");
        prompt.append("- Quantify impact using numbers, percentages, or scale where possible.\n");
        prompt.append("- Start each bullet with a strong, diverse action verb (e.g., Spearheaded, Orchestrated, Optimized).\n");
        prompt.append("- Tailor vocabulary to the specific Job Title provided.\n");
        prompt.append("- Keep each bullet concise (under 2 lines).\n");
        prompt.append("- Output ONLY the bulleted list (using  or -)  no headers or introductory text.");
        
        return prompt.toString();
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
            "Output only the improved content  no explanations.",
            sanitize(sectionType), sanitize(targetRole), sanitize(currentContent)
        );
    }

    public static String buildAtsPrompt(String resumeContent, String jobDescription) {
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            return String.format(
                "You are an ATS (Applicant Tracking System) expert. Analyse this resume based on general industry standards. " +
                "Analyse keywords, formatting, readability, missing skills, section completeness, and recruiter optimization. " +
                "Calculate a real ATS score dynamically. " +
                "Respond ONLY in this exact JSON format:\n" +
                "{\n" +
                "  \"score\": <integer 0-100>,\n" +
                "  \"missingKeywords\": [\"keyword1\", \"keyword2\", ...],\n" +
                "  \"recommendations\": \"<concise actionable recommendations covering formatting, readability, section completeness, and recruiter optimization>\"\n" +
                "}\n\n" +
                "Resume:\n%s",
                sanitize(resumeContent)
            );
        }
        return String.format(
            "You are an ATS (Applicant Tracking System) expert. Perform a deep semantic comparison between the resume and the job description. " +
            "Analyse keywords, formatting, readability, missing skills, section completeness, and recruiter optimization. " +
            "Calculate a real ATS score dynamically based on this semantic match. " +
            "Respond ONLY in this exact JSON format:\n" +
            "{\n" +
            "  \"score\": <integer 0-100>,\n" +
            "  \"missingKeywords\": [\"keyword1\", \"keyword2\", ...],\n" +
            "  \"recommendations\": \"<concise actionable recommendations based on the semantic match, formatting, readability, and missing skills>\"\n" +
            "}\n\n" +
            "Resume:\n%s\n\n" +
            "Job Description:\n%s",
            sanitize(resumeContent), sanitize(jobDescription)
        );
    }

    public static String buildSkillsPrompt(String jobTitle, String currentSkills, String industry) {
        return String.format(
            "You are a career advisor. Suggest exactly 10 in-demand skills for a %s in the %s industry.\n\n" +
            "The candidate already has: %s\n\n" +
            "Focus on skills they are MISSING.\n" +
            "CRITICAL: Output ONLY the exact skill names, one per line. Do not use bullet points, numbering, asterisks, bold text, categories, or headers. Just the raw skill names.",
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

    public static String buildChatPrompt(String userMessage, String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional AI career assistant for 'ResumeAI', a platform for resume building and job matching.\n\n");
        
        if (context != null && !context.isBlank()) {
            prompt.append("Relevant Context: ").append(sanitize(context)).append("\n\n");
        }
        
        prompt.append("User Question: ").append(sanitize(userMessage)).append("\n\n");
        prompt.append("Guidelines:\n");
        prompt.append("- Be professional, helpful, and concise.\n");
        prompt.append("- Provide actionable career advice or help with using the ResumeAI platform.\n");
        prompt.append("- If the user asks about resumes, interviews, or job matching, give expert-level suggestions.\n");
        prompt.append("- Keep responses under 3 paragraphs unless a detailed explanation is required.\n");
        prompt.append("- Do not use markdown headers (###), use bold text for emphasis instead.");
        
        return prompt.toString();
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
