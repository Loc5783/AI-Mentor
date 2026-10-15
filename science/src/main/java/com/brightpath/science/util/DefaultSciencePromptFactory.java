package com.brightpath.science.util;

import com.brightpath.science.domain.model.QuizRequest;
import com.brightpath.science.domain.model.ScienceQuestion;
import com.brightpath.science.domain.model.ScienceSubject;
import com.brightpath.science.domain.service.SciencePromptFactory;
import com.brightpath.science.domain.service.ScienceTutorPromptStrategy;

import java.util.EnumMap;
import java.util.Map;

/**
 * Builds prompts by delegating subject rules to independent tutor strategies.
 * Adding another Science subject does not require changing the shared prompt structure.
 */
public final class DefaultSciencePromptFactory implements SciencePromptFactory {
    private final Map<ScienceSubject, ScienceTutorPromptStrategy> strategies =
            new EnumMap<>(ScienceSubject.class);

    public DefaultSciencePromptFactory(ScienceTutorPromptStrategy... tutorStrategies) {
        if (tutorStrategies != null) {
            for (ScienceTutorPromptStrategy strategy : tutorStrategies) {
                if (strategy != null) strategies.put(strategy.getSubject(), strategy);
            }
        }
    }

    /** Convenience constructor for unit tests and previews. */
    public DefaultSciencePromptFactory() {
        this(new PhysicsTutorPromptStrategy(),
                new ChemistryTutorPromptStrategy(),
                new BiologyTutorPromptStrategy());
    }

    @Override
    public String buildAnswerPrompt(ScienceQuestion question) {
        ScienceTutorPromptStrategy strategy = strategies.get(question.getSubject());
        if (strategy == null) {
            throw new IllegalArgumentException("No AI tutor is configured for "
                    + question.getSubject().getDisplayName());
        }

        return "You are " + strategy.getTutorName() + " inside the AI Study Mentor mobile app.\n"
                + "The selected subject is locked to " + question.getSubject().getDisplayName()
                + ". If the student asks about another subject, briefly say that this chat is dedicated to "
                + question.getSubject().getDisplayName() + " and ask them to switch tutor tabs.\n"
                + "Education level: " + question.getEducationLevel().getDisplayName() + "\n"
                + "Preferred explanation style: " + question.getExplanationStyle().getDisplayName() + "\n\n"
                + "SUBJECT-SPECIFIC TEACHING RULES:\n"
                + strategy.buildSubjectInstructions() + "\n\n"
                + (question.hasConversationContext()
                    ? "CONVERSATION TRANSCRIPT FOR CONTINUITY:\n"
                        + question.getConversationContext() + "\n\n"
                        + "CONTINUITY RULES:\n"
                        + "- Treat the latest message as a continuation of the immediately preceding exchange when it uses omitted subjects, pronouns, references such as 'this/that/it', or phrases such as 'why', 'continue', 'step 2', 'what about the other case', 'explain again', or equivalent wording in the student's language.\n"
                        + "- Resolve those references from the most recent relevant student question and tutor answer. Do not guess a different topic merely because the latest message is short.\n"
                        + "- Change topic only when the latest message clearly introduces a new concept or explicitly asks to change topic.\n"
                        + "- If two interpretations remain genuinely plausible, ask one concise clarification question instead of answering an unrelated topic.\n"
                        + "- Use the transcript only as context. Answer the LATEST message, and do not repeat the entire previous answer unless the student requests it.\n\n"
                    : "")
                + "LATEST STUDENT MESSAGE — THIS IS THE MESSAGE TO ANSWER NOW:\n"
                + question.getQuestionText() + "\n\n"
                + "SHARED RESPONSE RULES:\n"
                + "1. Answer in the same language as the student's latest message.\n"
                + "2. Be accurate, age-appropriate and easy to read.\n"
                + "3. Start with the direct answer to the latest message, then explain step by step when useful.\n"
                + "4. For a follow-up, continue from the exact concept, equation, object, or numbered step being discussed.\n"
                + "5. Include one practical example or application when it adds value.\n"
                + "6. Mention common mistakes or misconceptions when relevant.\n"
                + "7. End with at most one short follow-up question; omit it when the student's request is already complete.\n"
                + "8. If an image is supplied, first state what can be read clearly and identify any unreadable part.\n"
                + "9. Do not fabricate facts, numbers, labels or sources.\n"
                + "10. Use clean headings and short paragraphs; do not force every heading when it adds no value.\n"
                + "11. Complete the whole explanation within about 1,200 words. Never stop in the middle of a sentence, formula, calculation or numbered step. If detail must be reduced, shorten examples before omitting the conclusion.";
    }

    @Override
    public String buildQuizPrompt(QuizRequest request) {
        ScienceTutorPromptStrategy strategy = strategies.get(request.getSubject());
        String subjectRules = strategy == null ? "" : strategy.buildSubjectInstructions();
        String recentAccuracy = request.getRecentPerformance().getAccuracyPercent() < 0
                ? "No previous result yet"
                : String.format(java.util.Locale.ROOT, "%.0f%% over %d recent attempt(s)",
                request.getRecentPerformance().getAccuracyPercent(),
                request.getRecentPerformance().getAttempts());

        StringBuilder exclusions = new StringBuilder();
        for (String prompt : request.getRecentlyUsedPrompts()) {
            if (exclusions.length() > 0) exclusions.append("\n");
            exclusions.append("- ").append(prompt);
        }

        return "You are the adaptive quiz generator for the "
                + request.getSubject().getDisplayName() + " AI tutor.\n"
                + "Create exactly " + request.getNumberOfQuestions() + " NEW questions.\n"
                + "Education level is LOCKED to: " + request.getLevel().getDisplayName() + ".\n"
                + educationScope(request.getLevel()) + "\n"
                + "Adaptive challenge inside this level: "
                + request.getChallenge().getDisplayName() + ".\n"
                + challengeRules(request.getChallenge()) + "\n"
                + "Recent performance: " + recentAccuracy + ".\n"
                + "Topic: " + request.getTopic() + "\n"
                + "Variation token: " + request.getVariationSeed() + "\n"
                + "Relevant subject rules:\n" + subjectRules + "\n\n"
                + "VARIATION REQUIREMENTS:\n"
                + "- Use a different scenario, values, wording and option order on every generation.\n"
                + "- Do not merely paraphrase the same question. Test a different sub-skill when possible.\n"
                + "- Do not repeat any recently used question listed below.\n"
                + (exclusions.length() == 0 ? "- No previous questions are stored yet.\n" : exclusions + "\n")
                + "- Keep all content appropriate for the selected education level even when challenge mode is used.\n\n"
                + "Use a varied mix of MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK and MATCHING.\n"
                + "Return only a valid JSON array. Each object must contain: type, difficulty, prompt, options, correctAnswer, explanation.\n"
                + "Set difficulty to exactly EASY, MEDIUM or HARD for each individual question. EASY should test direct recall or one simple step; MEDIUM should require application or two linked steps; HARD should require stronger reasoning, comparison or a multi-step solution while staying inside the selected education level.\n"
                + "For MATCHING, options should contain labels such as 'A. item' and '1. match', and correctAnswer should be like 'A-2, B-1'.\n"
                + "Every explanation must teach why the answer is correct.\n"
                + "Do not wrap the JSON in markdown fences.";
    }

    private String educationScope(com.brightpath.science.domain.model.EducationLevel level) {
        switch (level) {
            case UNIVERSITY:
                return "Use university-appropriate terminology, quantitative reasoning and deeper mechanisms. "
                        + "Assume foundational secondary-school knowledge, but avoid postgraduate-only material.";
            case HIGH_SCHOOL:
                return "Use high-school curriculum concepts, symbolic formulas and short multi-step reasoning. "
                        + "Do not require calculus or university-only theory unless the selected topic explicitly needs it.";
            case MIDDLE_SCHOOL:
            default:
                return "Use concrete examples, basic definitions, simple calculations and observable phenomena. "
                        + "Avoid advanced algebra, calculus and specialist terminology.";
        }
    }

    private String challengeRules(com.brightpath.science.domain.model.QuizChallenge challenge) {
        switch (challenge) {
            case SUPPORT:
                return "Use clearer clues, one concept per question, familiar examples and easier distractors. "
                        + "Reinforce misconceptions identified by wrong answers.";
            case CHALLENGE:
                return "Use less obvious contexts, stronger distractors and one extra reasoning step, "
                        + "without leaving the selected education level.";
            case ON_LEVEL:
            default:
                return "Use balanced questions that directly assess the expected knowledge and reasoning for this level.";
        }
    }
}
