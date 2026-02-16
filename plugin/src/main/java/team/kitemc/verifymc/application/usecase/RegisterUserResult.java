package team.kitemc.verifymc.application.usecase;

public record RegisterUserResult(boolean success, String messageKey, String outcome, String requestId) {
}
