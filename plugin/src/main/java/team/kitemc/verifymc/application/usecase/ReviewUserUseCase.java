package team.kitemc.verifymc.application.usecase;

import java.util.Map;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.service.ReviewApplicationService;

public class ReviewUserUseCase {
    private final UserDao userDao;
    private final ReviewApplicationService reviewApplicationService;

    public ReviewUserUseCase(UserDao userDao, ReviewApplicationService reviewApplicationService) {
        this.userDao = userDao;
        this.reviewApplicationService = reviewApplicationService;
    }

    public Result execute(Command command) {
        Map<String, Object> user = userDao.getUserByUuid(command.uuid());
        if (user == null) {
            throw UseCaseFailureException.business("ADMIN_USER_NOT_FOUND", "admin.user_not_found");
        }
        boolean approve = "approve".equals(command.action());
        boolean updated = userDao.updateUserStatus(command.uuid(), approve ? "approved" : "rejected");
        ReviewApplicationService.ReviewResult result = reviewApplicationService.buildReviewResponse(
                new ReviewApplicationService.ReviewCommand(updated, approve)
        );
        return new Result(result.success(), result.messageKey(), command.uuid(), approve);
    }

    public record Command(String uuid, String action) {}

    public record Result(boolean success, String messageKey, String uuid, boolean approve) {}
}
