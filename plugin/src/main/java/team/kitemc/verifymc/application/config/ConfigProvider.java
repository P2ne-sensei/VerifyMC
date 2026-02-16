package team.kitemc.verifymc.application.config;

import org.bukkit.configuration.file.FileConfiguration;

public interface ConfigProvider {
    VerifyMcConfig current();

    long version();

    FileConfiguration raw();

    ConfigVersionSnapshot reloadAndReplace(FileConfiguration rawConfig);

    record ConfigVersionSnapshot(long version, VerifyMcConfig config) {}
}
