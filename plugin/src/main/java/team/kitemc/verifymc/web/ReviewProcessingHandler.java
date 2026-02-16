package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import org.json.JSONObject;
import team.kitemc.verifymc.application.usecase.ReviewUserCommand;
import team.kitemc.verifymc.application.usecase.ReviewUserResult;
import team.kitemc.verifymc.application.usecase.ReviewUserUseCase;
import team.kitemc.verifymc.application.usecase.UseCaseFailureException;

public class ReviewProcessingHandler implements HttpHandler {
    private final ReviewUserUseCase reviewUserUseCase;
    private final BiFunction<String, String, String> messageResolver;

    public ReviewProcessingHandler(ReviewUserUseCase reviewUserUseCase, BiFunction<String, String, String> messageResolver) {
        this.reviewUserUseCase = reviewUserUseCase;
        this.messageResolver = messageResolver;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) {
            return;
        }

        JSONObject req = new JSONObject(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String language = req.optString("language", "en");
        ReviewUserCommand command = new ReviewUserCommand(
                req.optString("uuid"),
                req.optString("action"),
                req.optString("reason", ""),
                language
        );

        try {
            ReviewUserResult result = reviewUserUseCase.execute(command);
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.create(result.success(), messageResolver.apply(result.messageKey(), language)));
        } catch (UseCaseFailureException ex) {
            String message = messageResolver.apply(ex.messageKey(), language);
            WebResponseHelper.sendJson(exchange, ex.systemError()
                    ? ApiResponseFactory.systemError(message, ex.errorCode(), null)
                    : ApiResponseFactory.businessFailure(message, ex.errorCode(), null));
        } catch (Exception ex) {
            String message = messageResolver.apply("admin.load_failed", language);
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.systemError(message, "ADMIN_REVIEW_SYSTEM_ERROR", null));
        }
    }
}
