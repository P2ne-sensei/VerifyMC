package team.kitemc.verifymc.application.validation;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class QuestionnaireAnswerValidator {
    public boolean isSupportedQuestionType(String type) {
        return "single_choice".equals(type) || "multiple_choice".equals(type) || "text".equals(type);
    }

    public void validateAnswer(JSONObject questionDef, String answerType, List<Integer> selectedOptionIds, String textAnswer, int questionId) {
        boolean required = questionDef.optBoolean("required", false);
        JSONObject input = questionDef.optJSONObject("input");
        int minSelections = input != null ? input.optInt("min_selections", 0) : 0;
        int maxSelections = input != null ? input.optInt("max_selections", Integer.MAX_VALUE) : Integer.MAX_VALUE;
        int minLength = input != null ? input.optInt("min_length", 0) : 0;
        int maxLength = input != null ? input.optInt("max_length", Integer.MAX_VALUE) : Integer.MAX_VALUE;

        if ("single_choice".equals(answerType) || "multiple_choice".equals(answerType)) {
            JSONArray options = questionDef.optJSONArray("options");
            int optionCount = options != null ? options.length() : 0;
            if (required && selectedOptionIds.isEmpty()) {
                throw new IllegalArgumentException("Question " + questionId + " is required");
            }
            if (selectedOptionIds.size() < minSelections || selectedOptionIds.size() > maxSelections) {
                throw new IllegalArgumentException("Invalid selection count for question: " + questionId);
            }
            for (Integer optionId : selectedOptionIds) {
                if (optionId == null || optionId < 0 || optionId >= optionCount) {
                    throw new IllegalArgumentException("Invalid option id for question: " + questionId);
                }
            }
        } else if ("text".equals(answerType)) {
            String normalized = textAnswer != null ? textAnswer.trim() : "";
            if (required && normalized.isEmpty()) {
                throw new IllegalArgumentException("Question " + questionId + " is required");
            }
            if (!normalized.isEmpty() && (normalized.length() < minLength || normalized.length() > maxLength)) {
                throw new IllegalArgumentException("Invalid text length for question: " + questionId);
            }
        } else {
            throw new IllegalArgumentException("Unsupported question type: " + answerType);
        }
    }
}
