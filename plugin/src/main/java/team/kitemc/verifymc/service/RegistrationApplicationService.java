package team.kitemc.verifymc.service;

import team.kitemc.verifymc.registration.RegistrationOutcome;
import team.kitemc.verifymc.registration.RegistrationOutcomeMessageKeyMapper;
import team.kitemc.verifymc.registration.RegistrationOutcomeResolver;

public class RegistrationApplicationService {
    private final RegistrationOutcomeResolver resolver = new RegistrationOutcomeResolver();
    private final RegistrationOutcomeMessageKeyMapper messageKeyMapper = new RegistrationOutcomeMessageKeyMapper();

    public DecisionResult resolveDecision(DecisionCommand command) {
        boolean autoApprove = resolver.shouldAutoApprove(command.manualReviewRequired(), command.registerAutoApprove());
        RegistrationOutcome outcome = resolver.resolve(
                command.registerOk(),
                command.manualReviewRequired(),
                command.questionnairePassed(),
                command.registerAutoApprove(),
                command.scoringServiceUnavailable()
        );
        return new DecisionResult(autoApprove, outcome, resolver.resolveStatus(outcome));
    }

    public ResponseResult buildRegistrationResponse(ResponseCommand command) {
        String messageKey = messageKeyMapper.toMessageKey(command.decision().outcome());
        return new ResponseResult(command.registerOk(), messageKey, command.decision().outcome());
    }

    public record DecisionCommand(
            boolean registerOk,
            boolean manualReviewRequired,
            boolean questionnairePassed,
            boolean registerAutoApprove,
            boolean scoringServiceUnavailable
    ) {
    }

    public record DecisionResult(boolean autoApprove, RegistrationOutcome outcome, String status) {
    }

    public record ResponseCommand(DecisionResult decision, boolean registerOk) {
    }

    public record ResponseResult(boolean success, String messageKey, RegistrationOutcome outcome) {
    }
}
