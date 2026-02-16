package team.kitemc.verifymc.application.usecase;

public record ReviewUserResult(boolean success, String messageKey, String uuid, boolean approve) {
}
