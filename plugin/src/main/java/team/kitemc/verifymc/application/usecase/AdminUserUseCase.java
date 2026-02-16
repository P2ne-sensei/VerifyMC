package team.kitemc.verifymc.application.usecase;

import team.kitemc.verifymc.web.WebAuthHelper;

public class AdminUserUseCase {
    private final WebAuthHelper authHelper;

    public AdminUserUseCase(WebAuthHelper authHelper) {
        this.authHelper = authHelper;
    }

    public Result login(Command command) {
        if (command.expectedPassword().equals(command.password())) {
            return new Result(true, authHelper.generateSecureToken());
        }
        return new Result(false, null);
    }

    public record Command(String password, String expectedPassword) {}

    public record Result(boolean success, String token) {}
}
