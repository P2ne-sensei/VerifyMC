package team.kitemc.verifymc.application.usecase;

import org.json.JSONObject;

public class SubmitQuestionnaireUseCase {
    public Result execute(Command command) {
        if (command.answers() == null || command.answers().isEmpty()) {
            throw UseCaseFailureException.business("QUESTIONNAIRE_ANSWERS_REQUIRED", "questionnaire.answers_required");
        }
        return new Result(true, command.requestId(), command.answers());
    }

    public record Command(JSONObject answers, String requestId) {}

    public record Result(boolean success, String requestId, JSONObject answers) {}
}
