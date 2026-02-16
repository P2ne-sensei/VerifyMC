package team.kitemc.verifymc.bootstrap;

import java.nio.file.Path;
import java.util.ResourceBundle;
import team.kitemc.verifymc.ResourceManager;
import team.kitemc.verifymc.db.AuditDao;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.mail.MailService;
import team.kitemc.verifymc.service.AuthmeService;
import team.kitemc.verifymc.service.CaptchaService;
import team.kitemc.verifymc.service.DiscordService;
import team.kitemc.verifymc.service.QuestionnaireService;
import team.kitemc.verifymc.service.VerifyCodeService;
import team.kitemc.verifymc.service.VersionCheckService;
import team.kitemc.verifymc.web.ReviewWebSocketServer;
import team.kitemc.verifymc.web.WebServer;

public class ServiceRegistry {
    public ResourceBundle messages;
    public WebServer webServer;
    public ReviewWebSocketServer wsServer;
    public UserDao userDao;
    public AuditDao auditDao;
    public VerifyCodeService codeService;
    public MailService mailService;
    public AuthmeService authmeService;
    public VersionCheckService versionCheckService;
    public CaptchaService captchaService;
    public QuestionnaireService questionnaireService;
    public DiscordService discordService;
    public ResourceManager resourceManager;
    public String whitelistMode;
    public boolean whitelistJsonSync;
    public String webRegisterUrl;
    public String webServerPrefix;
    public Path whitelistJsonPath;
}
