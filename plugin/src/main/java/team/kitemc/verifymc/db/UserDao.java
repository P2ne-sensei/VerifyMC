package team.kitemc.verifymc.db;

import team.kitemc.verifymc.domain.user.QuestionnaireAudit;
import team.kitemc.verifymc.domain.user.User;

import java.util.List;
import java.util.Map;

public interface UserDao {
    boolean registerUser(String uuid, String username, String email, String status);

    boolean registerUser(String uuid, String username, String email, String status,
                         Integer questionnaireScore, Boolean questionnairePassed,
                         String questionnaireReviewSummary, Long questionnaireScoredAt);

    boolean registerUser(String uuid, String username, String email, String status, String password);

    boolean registerUser(String uuid, String username, String email, String status, String password,
                         Integer questionnaireScore, Boolean questionnairePassed,
                         String questionnaireReviewSummary, Long questionnaireScoredAt);

    boolean registerUser(User user);

    default boolean registerUser(User user, QuestionnaireAudit questionnaireAudit) {
        QuestionnaireAudit audit = questionnaireAudit == null ? user.questionnaireAudit() : questionnaireAudit;
        return registerUser(
                user.uuid(),
                user.username(),
                user.email(),
                user.status().value(),
                user.password(),
                audit.score(),
                audit.passed(),
                audit.reviewSummary(),
                audit.scoredAt()
        );
    }

    boolean updateUserStatus(String uuidOrName, String status);

    boolean updateUserPassword(String uuidOrName, String password);

    boolean updateUserEmail(String uuidOrName, String email);

    List<User> getAllUsersTyped();

    List<User> getUsersWithPaginationTyped(int page, int pageSize);

    int getTotalUserCount();

    List<User> getUsersWithPaginationAndSearchTyped(int page, int pageSize, String searchQuery);

    int getTotalUserCountWithSearch(String searchQuery);

    int getApprovedUserCount();

    int getApprovedUserCountWithSearch(String searchQuery);

    List<User> getApprovedUsersWithPaginationTyped(int page, int pageSize);

    List<User> getApprovedUsersWithPaginationAndSearchTyped(int page, int pageSize, String searchQuery);

    User getUserByUuidTyped(String uuid);

    User getUserByUsernameTyped(String username);

    boolean deleteUser(String uuidOrName);

    void save();

    int countUsersByEmail(String email);

    List<User> getPendingUsersTyped();

    boolean updateUserDiscordId(String uuidOrName, String discordId);

    User getUserByDiscordIdTyped(String discordId);

    boolean isDiscordIdLinked(String discordId);

    @Deprecated
    List<Map<String, Object>> getAllUsers();

    @Deprecated
    List<Map<String, Object>> getUsersWithPagination(int page, int pageSize);

    @Deprecated
    List<Map<String, Object>> getUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery);

    @Deprecated
    List<Map<String, Object>> getApprovedUsersWithPagination(int page, int pageSize);

    @Deprecated
    List<Map<String, Object>> getApprovedUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery);

    @Deprecated
    Map<String, Object> getUserByUuid(String uuid);

    @Deprecated
    Map<String, Object> getUserByUsername(String username);

    @Deprecated
    List<Map<String, Object>> getPendingUsers();

    @Deprecated
    Map<String, Object> getUserByDiscordId(String discordId);
}
