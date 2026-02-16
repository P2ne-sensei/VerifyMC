package team.kitemc.verifymc.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ApplicationServiceTest {

    @Test
    void registrationSuccessPathShouldReturnSuccessMessage() {
        RegistrationApplicationService service = new RegistrationApplicationService();
        RegistrationApplicationService.DecisionResult decision = service.resolveDecision(
                new RegistrationApplicationService.DecisionCommand(true, false, true, true, false)
        );
        RegistrationApplicationService.ResponseResult response = service.buildRegistrationResponse(
                new RegistrationApplicationService.ResponseCommand(decision, true)
        );

        assertTrue(response.success());
        assertEquals("register.success_whitelisted", response.messageKey());
    }

    @Test
    void autoApproveWithManualReviewFlagAndFailedQuestionnaireShouldGoPendingReview() {
        RegistrationApplicationService service = new RegistrationApplicationService();
        RegistrationApplicationService.DecisionResult decision = service.resolveDecision(
                new RegistrationApplicationService.DecisionCommand(true, true, false, true, false)
        );
        RegistrationApplicationService.ResponseResult response = service.buildRegistrationResponse(
                new RegistrationApplicationService.ResponseCommand(decision, true)
        );

        assertTrue(response.success());
        assertEquals("register.questionnaire_pending_review", response.messageKey());
    }

    @Test
    void scoringServiceUnavailableShouldUseScoringErrorMessage() {
        RegistrationApplicationService service = new RegistrationApplicationService();
        RegistrationApplicationService.DecisionResult decision = service.resolveDecision(
                new RegistrationApplicationService.DecisionCommand(true, true, false, true, true)
        );
        RegistrationApplicationService.ResponseResult response = service.buildRegistrationResponse(
                new RegistrationApplicationService.ResponseCommand(decision, true)
        );

        assertTrue(response.success());
        assertEquals("register.questionnaire_scoring_error_pending_review", response.messageKey());
    }

    @Test
    void autoApproveWithPassedQuestionnaireShouldStillWhitelist() {
        RegistrationApplicationService service = new RegistrationApplicationService();
        RegistrationApplicationService.DecisionResult decision = service.resolveDecision(
                new RegistrationApplicationService.DecisionCommand(true, false, true, true, false)
        );

        assertTrue(decision.autoApprove());
    }

    @Test
    void reviewApprovePathShouldReturnApprovedMessage() {
        ReviewApplicationService service = new ReviewApplicationService();
        ReviewApplicationService.ReviewResult response = service.buildReviewResponse(
                new ReviewApplicationService.ReviewCommand(true, true)
        );

        assertTrue(response.success());
        assertEquals("review.approve_success", response.messageKey());
    }

    @Test
    void reviewRejectPathShouldReturnRejectedMessage() {
        ReviewApplicationService service = new ReviewApplicationService();
        ReviewApplicationService.ReviewResult response = service.buildReviewResponse(
                new ReviewApplicationService.ReviewCommand(true, false)
        );

        assertTrue(response.success());
        assertEquals("review.reject_success", response.messageKey());
    }
}
