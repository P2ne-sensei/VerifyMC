package team.kitemc.verifymc.service;

public class ReviewApplicationService {
    public ReviewResult buildReviewResponse(ReviewCommand command) {
        if (!command.reviewSuccess()) {
            return new ReviewResult(false, "review.failed");
        }
        String key = command.approve() ? "review.approve_success" : "review.reject_success";
        return new ReviewResult(true, key);
    }

    public record ReviewCommand(boolean reviewSuccess, boolean approve) {
    }

    public record ReviewResult(boolean success, String messageKey) {
    }
}
