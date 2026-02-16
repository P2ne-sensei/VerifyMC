package team.kitemc.verifymc.application.usecase;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.mail.MailService;
import team.kitemc.verifymc.service.AuthmeService;
import team.kitemc.verifymc.service.ReviewApplicationService;

public class ReviewUserUseCase {
    private final Plugin plugin;
    private final UserDao userDao;
    private final AuthmeService authmeService;
    private final MailService mailService;
    private final ReviewApplicationService reviewApplicationService;
    private final Consumer<String> websocketBroadcaster;
    private final Consumer<String> debugLogger;

    public ReviewUserUseCase(
            Plugin plugin,
            UserDao userDao,
            AuthmeService authmeService,
            MailService mailService,
            ReviewApplicationService reviewApplicationService,
            Consumer<String> websocketBroadcaster,
            Consumer<String> debugLogger
    ) {
        this.plugin = plugin;
        this.userDao = userDao;
        this.authmeService = authmeService;
        this.mailService = mailService;
        this.reviewApplicationService = reviewApplicationService;
        this.websocketBroadcaster = websocketBroadcaster;
        this.debugLogger = debugLogger;
    }

    public ReviewUserResult execute(ReviewUserCommand command) {
        validateCommand(command);

        Map<String, Object> user = userDao.getUserByUuid(command.uuid());
        if (user == null) {
            throw UseCaseFailureException.business("ADMIN_USER_NOT_FOUND", "admin.user_not_found");
        }

        boolean approve = "approve".equals(command.action());
        boolean updated = userDao.updateUserStatus(command.uuid(), approve ? "approved" : "rejected");
        ReviewApplicationService.ReviewResult result = reviewApplicationService.buildReviewResponse(
                new ReviewApplicationService.ReviewCommand(updated, approve)
        );

        if (result.success()) {
            handleSideEffects(user, command, approve);
        }

        return new ReviewUserResult(result.success(), result.messageKey(), command.uuid(), approve);
    }

    private void validateCommand(ReviewUserCommand command) {
        if (command.uuid() == null || command.uuid().isBlank()) {
            throw UseCaseFailureException.business("ADMIN_INVALID_UUID", "admin.invalid_uuid");
        }
        try {
            UUID.fromString(command.uuid());
        } catch (IllegalArgumentException ex) {
            throw UseCaseFailureException.business("ADMIN_INVALID_UUID", "admin.invalid_uuid");
        }
        if (!"approve".equals(command.action()) && !"reject".equals(command.action())) {
            throw UseCaseFailureException.business("ADMIN_INVALID_ACTION", "admin.invalid_action");
        }
    }

    private void handleSideEffects(Map<String, Object> user, ReviewUserCommand command, boolean approve) {
        String username = (String) user.get("username");
        String password = (String) user.get("password");
        String userEmail = (String) user.get("email");

        if (username != null && !username.isBlank()) {
            if (approve) {
                debugLogger.accept("Execute: whitelist add " + username);
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist add " + username));
                if (authmeService.isAuthmeEnabled() && password != null && !password.trim().isEmpty()) {
                    authmeService.registerToAuthme(username, password);
                }
            } else {
                debugLogger.accept("Execute: whitelist remove " + username);
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist remove " + username));
                if (authmeService.isAuthmeEnabled()) {
                    authmeService.unregisterFromAuthme(username);
                }
            }
        }

        if (userEmail != null && !userEmail.trim().isEmpty()) {
            new Thread(() -> {
                try {
                    mailService.sendReviewResultNotification(userEmail, username, approve, command.reason(), command.language());
                } catch (Exception e) {
                    debugLogger.accept("Failed to send review result notification: " + e.getMessage());
                }
            }).start();
        }

        JSONObject wsMsg = new JSONObject();
        wsMsg.put("type", command.action());
        wsMsg.put("uuid", command.uuid());
        websocketBroadcaster.accept(wsMsg.toString());
    }
}
