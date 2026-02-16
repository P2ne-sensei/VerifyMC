package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.function.BiFunction;
import team.kitemc.verifymc.api.adapter.HttpExchangeAdapter;
import team.kitemc.verifymc.application.usecase.RegisterUserCommand;
import team.kitemc.verifymc.application.usecase.RegisterUserResult;
import team.kitemc.verifymc.application.usecase.RegisterUserUseCase;
import team.kitemc.verifymc.application.usecase.UseCaseFailureException;
import org.json.JSONObject;

public class RegistrationProcessingHandler implements HttpHandler {
    public static final long QUESTIONNAIRE_SUBMISSION_TTL_MS = 10 * 60 * 1000;

    private final RegisterUserUseCase registerUserUseCase;
    private final HttpExchangeAdapter httpExchangeAdapter;
    private final BiFunction<String, String, String> messageResolver;
    private final BiFunction<String, String, String> usernameNormalizer;

    public RegistrationProcessingHandler(
            RegisterUserUseCase registerUserUseCase,
            HttpExchangeAdapter httpExchangeAdapter,
            BiFunction<String, String, String> messageResolver,
            BiFunction<String, String, String> usernameNormalizer
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.httpExchangeAdapter = httpExchangeAdapter;
        this.messageResolver = messageResolver;
        this.usernameNormalizer = usernameNormalizer;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestId = httpExchangeAdapter.resolveRequestId(exchange);

        if (!WebResponseHelper.requireMethod(exchange, "POST")) {
            return;
        }

        RegistrationRequest request = httpExchangeAdapter.readRegistrationRequest(exchange, usernameNormalizer);
        RegisterUserCommand command = new RegisterUserCommand(
                request.email(),
                request.code(),
                request.uuid(),
                request.username(),
                request.normalizedUsername(),
                request.password(),
                request.captchaToken(),
                request.captchaAnswer(),
                request.language(),
                request.platform(),
                request.questionnaire(),
                requestId
        );

        try {
            RegisterUserResult result = registerUserUseCase.execute(command);
            String message = messageResolver.apply(result.messageKey(), request.language());
            JSONObject response = ApiResponseFactory.create(result.success(), message, result.success() ? ApiResponseFactory.ERROR_CODE_NONE : ApiResponseFactory.ERROR_CODE_BUSINESS, requestId)
                    .put("outcome", result.outcome());
            httpExchangeAdapter.writeSuccess(exchange, response);
        } catch (UseCaseFailureException ex) {
            httpExchangeAdapter.writeUseCaseFailure(exchange, ex, request.language(), requestId, key -> messageResolver.apply(key, request.language()));
        } catch (Exception ex) {
            UseCaseFailureException systemFailure = UseCaseFailureException.system("REGISTER_SYSTEM_ERROR", "register.failed");
            httpExchangeAdapter.writeUseCaseFailure(exchange, systemFailure, request.language(), requestId, key -> messageResolver.apply(key, request.language()));
        }
    }
}
