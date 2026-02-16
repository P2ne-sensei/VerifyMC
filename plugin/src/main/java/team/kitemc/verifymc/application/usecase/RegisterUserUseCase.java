package team.kitemc.verifymc.application.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.json.JSONArray;
import org.json.JSONObject;
import team.kitemc.verifymc.application.config.ConfigProvider;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.service.AuthmeService;
import team.kitemc.verifymc.service.CaptchaService;
import team.kitemc.verifymc.service.DiscordService;
import team.kitemc.verifymc.service.FeatureFlagService;
import team.kitemc.verifymc.service.QuestionnaireService;
import team.kitemc.verifymc.service.RegistrationApplicationService;
import team.kitemc.verifymc.service.VerifyCodeService;
import team.kitemc.verifymc.web.RegistrationProcessingHandler;
import team.kitemc.verifymc.web.RegistrationRequest;

public class RegisterUserUseCase {
    private final Plugin plugin;
    private final ConfigProvider configProvider;
    private final VerifyCodeService codeService;
    private final UserDao userDao;
    private final AuthmeService authmeService;
    private final CaptchaService captchaService;
    private final QuestionnaireService questionnaireService;
    private final DiscordService discordService;
    private final FeatureFlagService featureFlagService;
    private final RegistrationApplicationService registrationApplicationService;
    private final Map<String, RegistrationProcessingHandler.QuestionnaireSubmissionRecord> questionnaireSubmissionStore;
    private final Supplier<List<String>> emailDomainWhitelistProvider;
    private final BiFunction<String, String, String> usernameRegexResolver;
    private final BiPredicate<String, String> usernameValidator;
    private final Function<String, Boolean> usernameCaseConflictChecker;
    private final Function<String, Boolean> emailValidator;
    private final Function<String, Boolean> uuidValidator;
    private final Consumer<String> debugLogger;

    public RegisterUserUseCase(
            Plugin plugin,
            ConfigProvider configProvider,
            VerifyCodeService codeService,
            UserDao userDao,
            AuthmeService authmeService,
            CaptchaService captchaService,
            QuestionnaireService questionnaireService,
            DiscordService discordService,
            FeatureFlagService featureFlagService,
            RegistrationApplicationService registrationApplicationService,
            Map<String, RegistrationProcessingHandler.QuestionnaireSubmissionRecord> questionnaireSubmissionStore,
            Supplier<List<String>> emailDomainWhitelistProvider,
            BiFunction<String, String, String> usernameRegexResolver,
            BiPredicate<String, String> usernameValidator,
            Function<String, Boolean> usernameCaseConflictChecker,
            Function<String, Boolean> emailValidator,
            Function<String, Boolean> uuidValidator,
            Consumer<String> debugLogger
    ) {
        this.plugin = plugin;
        this.configProvider = configProvider;
        this.codeService = codeService;
        this.userDao = userDao;
        this.authmeService = authmeService;
        this.captchaService = captchaService;
        this.questionnaireService = questionnaireService;
        this.discordService = discordService;
        this.featureFlagService = featureFlagService;
        this.registrationApplicationService = registrationApplicationService;
        this.questionnaireSubmissionStore = questionnaireSubmissionStore;
        this.emailDomainWhitelistProvider = emailDomainWhitelistProvider;
        this.usernameRegexResolver = usernameRegexResolver;
        this.usernameValidator = usernameValidator;
        this.usernameCaseConflictChecker = usernameCaseConflictChecker;
        this.emailValidator = emailValidator;
        this.uuidValidator = uuidValidator;
        this.debugLogger = debugLogger;
    }

    public Result execute(Command command) {
        RegistrationRequest request = command.request();
        validateBasicInput(request, command.requestId());
        RegistrationProcessingHandler.QuestionnaireSubmissionRecord questionnaireSubmissionRecord =
                validateQuestionnaireSubmission(request, command.requestId());
        validateVerificationMethod(request, command.requestId());
        validateDiscordRequirement(request, command.requestId());

        boolean manualReviewRequired = questionnaireSubmissionRecord != null && questionnaireSubmissionRecord.manualReviewRequired();
        boolean questionnairePassed = questionnaireSubmissionRecord != null && questionnaireSubmissionRecord.passed();
        boolean scoringServiceUnavailable = questionnaireSubmissionRecord != null && questionnaireSubmissionRecord.scoringServiceUnavailable();
        boolean registerAutoApprove = configProvider.current().registerAutoApprove();

        RegistrationApplicationService.DecisionCommand decisionCommand = new RegistrationApplicationService.DecisionCommand(
                true,
                manualReviewRequired,
                questionnairePassed,
                registerAutoApprove,
                scoringServiceUnavailable
        );
        RegistrationApplicationService.DecisionResult preDecision = registrationApplicationService.resolveDecision(decisionCommand);

        Integer questionnaireScore = questionnaireSubmissionRecord != null ? questionnaireSubmissionRecord.score() : null;
        Boolean questionnairePassedValue = questionnaireSubmissionRecord != null ? questionnaireSubmissionRecord.passed() : null;
        String questionnaireReviewSummary = questionnaireSubmissionRecord != null ? buildQuestionnaireReviewSummary(questionnaireSubmissionRecord.details()) : null;
        Long questionnaireScoredAt = questionnaireSubmissionRecord != null ? questionnaireSubmissionRecord.submittedAt() : null;

        boolean ok = registerUserToDao(request, preDecision.status(), questionnaireScore, questionnairePassedValue, questionnaireReviewSummary, questionnaireScoredAt);

        RegistrationApplicationService.DecisionResult decision = registrationApplicationService.resolveDecision(
                new RegistrationApplicationService.DecisionCommand(
                        ok,
                        manualReviewRequired,
                        questionnairePassed,
                        registerAutoApprove,
                        scoringServiceUnavailable
                )
        );

        if (decision.outcome().name().equals("SUCCESS_WHITELISTED")) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist add " + request.normalizedUsername()));
            if (authmeService.isAuthmeEnabled() && request.password() != null && !request.password().trim().isEmpty()) {
                authmeService.registerToAuthme(request.normalizedUsername(), request.password());
            }
        }

        RegistrationApplicationService.ResponseResult responseResult = registrationApplicationService.buildRegistrationResponse(
                new RegistrationApplicationService.ResponseCommand(decision, ok)
        );
        return new Result(responseResult.success(), responseResult.messageKey(), decision.outcome().name(), command.requestId());
    }

    private boolean registerUserToDao(
            RegistrationRequest request,
            String status,
            Integer questionnaireScore,
            Boolean questionnairePassedValue,
            String questionnaireReviewSummary,
            Long questionnaireScoredAt
    ) {
        if (request.password() != null && !request.password().trim().isEmpty()) {
            String storedPassword = authmeService.encodePasswordForStorage(request.password());
            return userDao.registerUser(request.uuid(), request.normalizedUsername(), request.email(), status, storedPassword,
                    questionnaireScore, questionnairePassedValue, questionnaireReviewSummary, questionnaireScoredAt);
        }
        return userDao.registerUser(request.uuid(), request.normalizedUsername(), request.email(), status,
                questionnaireScore, questionnairePassedValue, questionnaireReviewSummary, questionnaireScoredAt);
    }

    private void validateBasicInput(RegistrationRequest request, String requestId) {
        logRegistrationStage(requestId, "validate_basic_input", null);
        if (request.email().isEmpty() || request.code().isEmpty() || request.uuid().isEmpty()) {
            throw UseCaseFailureException.business("REGISTER_REQUIRED_FIELDS", "register.required");
        }
        if (configProvider.current().auth().emailAliasLimitEnabled() && request.email().contains("+")) {
            throw UseCaseFailureException.business("REGISTER_ALIAS_NOT_ALLOWED", "register.alias_not_allowed");
        }
        if (configProvider.current().auth().emailDomainWhitelistEnabled()) {
            String domain = request.email().contains("@") ? request.email().substring(request.email().indexOf('@') + 1) : "";
            if (!emailDomainWhitelistProvider.get().contains(domain)) {
                throw UseCaseFailureException.business("REGISTER_DOMAIN_NOT_ALLOWED", "register.domain_not_allowed");
            }
        }
        if (userDao.getUserByUsername(request.normalizedUsername()) != null) {
            throw UseCaseFailureException.business("REGISTER_USERNAME_EXISTS", "register.username_exists");
        }
        if (!usernameValidator.test(request.normalizedUsername(), request.platform())) {
            String usernameRegex = usernameRegexResolver.apply(request.normalizedUsername(), request.platform());
            throw UseCaseFailureException.business("REGISTER_INVALID_USERNAME", "username.invalid", new JSONObject().put("regex", usernameRegex));
        }
        if (usernameCaseConflictChecker.apply(request.normalizedUsername())) {
            throw UseCaseFailureException.business("REGISTER_USERNAME_CASE_CONFLICT", "username.case_conflict");
        }

        int maxAccounts = configProvider.current().maxAccountsPerEmail();
        int emailCount = userDao.countUsersByEmail(request.email());
        if (emailCount >= maxAccounts) {
            throw UseCaseFailureException.business("REGISTER_EMAIL_LIMIT", "register.email_limit");
        }
        if (!emailValidator.apply(request.email())) {
            throw UseCaseFailureException.business("REGISTER_INVALID_EMAIL", "register.invalid_email");
        }
        if (!uuidValidator.apply(request.uuid())) {
            throw UseCaseFailureException.business("REGISTER_INVALID_UUID", "register.invalid_uuid");
        }
        if (request.normalizedUsername() == null || request.normalizedUsername().trim().isEmpty()) {
            throw UseCaseFailureException.business("REGISTER_INVALID_USERNAME", "register.invalid_username");
        }
    }

    private RegistrationProcessingHandler.QuestionnaireSubmissionRecord validateQuestionnaireSubmission(RegistrationRequest request, String requestId) {
        logRegistrationStage(requestId, "validate_questionnaire_submission", null);
        if (!featureFlagService.isQuestionnaireEnabled()) {
            return null;
        }

        JSONObject questionnaire = request.questionnaire();
        if (questionnaire == null) {
            throw UseCaseFailureException.business("REGISTER_QUESTIONNAIRE_REQUIRED", "register.questionnaire_required");
        }

        String questionnaireToken = questionnaire.optString("token", "");
        long submittedAt = questionnaire.optLong("submitted_at", 0L);
        long expiresAt = questionnaire.optLong("expires_at", 0L);
        JSONObject answers = questionnaire.optJSONObject("answers");

        if (questionnaireToken.isEmpty() || answers == null) {
            throw UseCaseFailureException.business("REGISTER_QUESTIONNAIRE_REQUIRED", "register.questionnaire_required");
        }

        RegistrationProcessingHandler.QuestionnaireSubmissionRecord record = questionnaireSubmissionStore.remove(questionnaireToken);
        if (record == null) {
            throw UseCaseFailureException.business("REGISTER_QUESTIONNAIRE_MISSING", "register.questionnaire_missing");
        }

        if (record.isExpired() || System.currentTimeMillis() > expiresAt || submittedAt <= 0 || expiresAt <= submittedAt) {
            throw UseCaseFailureException.business("REGISTER_QUESTIONNAIRE_EXPIRED", "register.questionnaire_expired");
        }

        if (!record.answers().similar(answers) || record.submittedAt() != submittedAt || record.expiresAt() != expiresAt) {
            throw UseCaseFailureException.business("REGISTER_QUESTIONNAIRE_INVALID", "register.questionnaire_invalid");
        }

        if (!record.passed() && !record.manualReviewRequired()) {
            throw UseCaseFailureException.business("REGISTER_QUESTIONNAIRE_REQUIRED", "register.questionnaire_required");
        }
        return record;
    }

    private void validateVerificationMethod(RegistrationRequest request, String requestId) {
        logRegistrationStage(requestId, "validate_verification_method", null);
        List<String> authMethods = configProvider.raw().getStringList("auth_methods");
        boolean useCaptcha = authMethods.contains("captcha");
        boolean useEmail = authMethods.contains("email");

        if (useCaptcha) {
            if (request.captchaToken().isEmpty() || request.captchaAnswer().isEmpty()) {
                throw UseCaseFailureException.business("CAPTCHA_REQUIRED", "captcha.required");
            }
            if (!captchaService.validateCaptcha(request.captchaToken(), request.captchaAnswer())) {
                throw UseCaseFailureException.business("CAPTCHA_INVALID", "captcha.invalid");
            }
        }

        if (useEmail || !useCaptcha) {
            if (!codeService.checkCode(request.email(), request.code())) {
                throw UseCaseFailureException.business("VERIFY_WRONG_CODE", "verify.wrong_code");
            }
        }
    }

    private void validateDiscordRequirement(RegistrationRequest request, String requestId) {
        logRegistrationStage(requestId, "validate_discord_requirement", null);
        if (featureFlagService.isDiscordRequired() && !discordService.isLinked(request.normalizedUsername())) {
            throw UseCaseFailureException.business("DISCORD_REQUIRED", "discord.required", new JSONObject().put("discord_required", true));
        }
    }

    private String buildQuestionnaireReviewSummary(JSONArray details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < details.length(); i++) {
            JSONObject detail = details.optJSONObject(i);
            if (detail == null || !"text".equalsIgnoreCase(detail.optString("type", ""))) {
                continue;
            }
            int questionId = detail.optInt("question_id", -1);
            int score = detail.optInt("score", 0);
            int maxScore = detail.optInt("max_score", 0);
            String reason = detail.optString("reason", "").trim();
            if (reason.isEmpty()) {
                reason = "N/A";
            }
            parts.add("Q" + questionId + "(" + score + "/" + maxScore + "): " + reason);
        }
        return parts.isEmpty() ? null : String.join(" | ", parts);
    }

    private void logRegistrationStage(String requestId, String stage, JSONObject extra) {
        JSONObject payload = new JSONObject();
        payload.put("requestId", requestId);
        payload.put("stage", stage);
        if (extra != null) {
            payload.put("extra", extra);
        }
        debugLogger.accept("registration_stage=" + payload);
    }

    public record Command(RegistrationRequest request, String requestId) {}

    public record Result(boolean success, String messageKey, String outcome, String requestId) {}
}
