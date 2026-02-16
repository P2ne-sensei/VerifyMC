package team.kitemc.verifymc.application.usecase;

public record ReviewUserCommand(String uuid, String action, String reason, String language) {
}
