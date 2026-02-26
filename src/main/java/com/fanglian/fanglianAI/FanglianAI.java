package com.fanglian.fanglianAI;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FanglianAI extends JavaPlugin {

    private static FanglianAI instance;
    private FlaiCommand commandHandler;

    // 常量定义
    private static final String PERMISSION_PREFIX = "fanglianai.";
    public static final String PERM_USE = PERMISSION_PREFIX + "use";
    public static final String PERM_TOKEN = PERMISSION_PREFIX + "token";
    public static final String PERM_PRIVATE = PERMISSION_PREFIX + "private";
    public static final String PERM_PRESET = PERMISSION_PREFIX + "preset";
    public static final String PERM_RELOAD = PERMISSION_PREFIX + "reload";
    public static final String PERM_BYPASS_LIMIT = PERMISSION_PREFIX + "bypass.limit";
    public static final String PERM_BYPASS_COOLDOWN = PERMISSION_PREFIX + "bypass.cooldown";

    // 配置键常量
    private static final String CFG_API_KEY = "api.key";
    private static final String CFG_API_URL = "api.url";
    private static final String CFG_API_MODEL = "api.model";
    private static final String CFG_API_MODELS = "api.available-models";
    private static final String CFG_CONTEXT_ENABLED = "context.enabled";
    private static final String CFG_CONTEXT_MAX = "context.max-messages";
    private static final String CFG_TOKEN_ENABLED = "token-limit.enabled";
    private static final String CFG_TOKEN_LIMIT = "token-limit.daily-limit";
    private static final String CFG_COOLDOWN_ENABLED = "cooldown.enabled";
    private static final String CFG_COOLDOWN_SECONDS = "cooldown.seconds";
    private static final String CFG_COOLDOWN_BYPASS = "cooldown.bypass-permission";
    private static final String CFG_PRIVATE_DEFAULT = "private-mode.default-enabled";
    private static final String CFG_SYSTEM_ENABLED = "system-prompt.enabled";
    private static final String CFG_SYSTEM_CONTENT = "system-prompt.content";
    private static final String CFG_MC_SYSTEM_CONTENT = "mc-system-prompt.content";

    @Override
    public void onEnable() {
        instance = this;

        // 保存默认配置
        saveDefaultConfig();

        String version = getDescription().getVersion();
        getLogger().info("========================================");
        getLogger().info("  fanglianAI v" + version + " 启动中...");
        getLogger().info("  作者: xcs324");
        getLogger().info("========================================");

        // 初始化命令处理器
        commandHandler = new FlaiCommand();
        getCommand("flai").setExecutor(commandHandler);
        getCommand("flai").setTabCompleter(commandHandler);

        // 启动每日0点重置Token的定时任务
        startDailyResetTask();

        getLogger().info("fanglianAI 插件已启用！使用 /flai help 查看帮助");
    }

    @Override
    public void onDisable() {
        getLogger().info("fanglianAI 插件已禁用！");
    }

    /**
     * 启动每日0点重置Token使用量的定时任务
     */
    private void startDailyResetTask() {
        // 每分钟检查一次是否到了0点
        new org.bukkit.scheduler.BukkitRunnable() {
            private String lastDate = java.time.LocalDate.now().toString();

            @Override
            public void run() {
                String currentDate = java.time.LocalDate.now().toString();
                if (!currentDate.equals(lastDate)) {
                    lastDate = currentDate;
                    if (commandHandler != null) {
                        commandHandler.resetDailyTokenUsage();
                        getLogger().info("新的一天开始，每日 Token 使用量已重置！");
                    }
                }
            }
        }.runTaskTimer(this, 20 * 60, 20 * 60); // 每分钟检查一次
    }

    public static FanglianAI getInstance() {
        return instance;
    }

    // ==================== API 配置 ====================

    public String getApiKey() {
        return getConfig().getString(CFG_API_KEY, "");
    }

    public String getApiUrl() {
        return getConfig().getString(CFG_API_URL, "https://apis.iflow.cn/v1/chat/completions");
    }

    public String getApiModel() {
        return getConfig().getString(CFG_API_MODEL, "deepseek-v3.2");
    }

    public List<String> getAvailableModels() {
        return getConfig().getStringList(CFG_API_MODELS);
    }

    // ==================== 上下文配置 ====================

    public boolean isContextEnabled() {
        return getConfig().getBoolean(CFG_CONTEXT_ENABLED, true);
    }

    public int getMaxContextMessages() {
        return getConfig().getInt(CFG_CONTEXT_MAX, 20);
    }

    // ==================== Token 限制配置 ====================

    public boolean isTokenLimitEnabled() {
        return getConfig().getBoolean(CFG_TOKEN_ENABLED, true);
    }

    public int getDailyTokenLimit() {
        return getConfig().getInt(CFG_TOKEN_LIMIT, 10000);
    }

    // ==================== 冷却时间配置 ====================

    public boolean isCooldownEnabled() {
        return getConfig().getBoolean(CFG_COOLDOWN_ENABLED, true);
    }

    public int getCooldownSeconds() {
        return getConfig().getInt(CFG_COOLDOWN_SECONDS, 5);
    }

    public String getCooldownBypassPermission() {
        return getConfig().getString(CFG_COOLDOWN_BYPASS, PERM_BYPASS_COOLDOWN);
    }

    // ==================== 私聊模式配置 ====================

    public boolean isPrivateModeDefault() {
        return getConfig().getBoolean(CFG_PRIVATE_DEFAULT, true);
    }

    // ==================== 系统提示词配置 ====================

    public boolean isSystemPromptEnabled() {
        return getConfig().getBoolean(CFG_SYSTEM_ENABLED, true);
    }

    public String getSystemPromptContent() {
        return getConfig().getString(CFG_SYSTEM_CONTENT, "");
    }

    // MC专家模式系统提示词配置
    public String getMcSystemPromptContent() {
        return getConfig().getString(CFG_MC_SYSTEM_CONTENT,
            "你是 Minecraft 游戏专家，专注于解答 Minecraft 相关问题。请用简洁清晰的中文回答，使用游戏内术语。");
    }

    // ==================== 预设指令配置 ====================

    public boolean isPresetEnabled(String presetName) {
        return getConfig().getBoolean("presets." + presetName + ".enabled", false);
    }

    public String getPresetPrompt(String presetName) {
        return getConfig().getString("presets." + presetName + ".prompt", "");
    }

    public Set<String> getPresetNames() {
        ConfigurationSection section = getConfig().getConfigurationSection("presets");
        if (section == null) {
            return Set.of();
        }
        return section.getKeys(false);
    }

    public Map<String, String> getEnabledPresets() {
        Map<String, String> presets = new HashMap<>();
        for (String name : getPresetNames()) {
            if (isPresetEnabled(name)) {
                presets.put(name, getPresetPrompt(name));
            }
        }
        return presets;
    }

    // ==================== 消息配置 ====================

    public String getMessage(String key) {
        return getConfig().getString("messages." + key, "");
    }

    public String getMessage(String key, String placeholder, String value) {
        String msg = getMessage(key);
        return msg.replace("{" + placeholder + "}", value);
    }

    public String getMessage(String key, String... placeholders) {
        String msg = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return msg;
    }

    // ==================== 配置重载 ====================

    public void reloadConfiguration() {
        reloadConfig();
        getLogger().info("配置文件已重新加载");
    }
}