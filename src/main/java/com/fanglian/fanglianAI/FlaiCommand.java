package com.fanglian.fanglianAI;

import com.google.gson.*;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FlaiCommand implements CommandExecutor, TabCompleter {

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build();

    // 普通模式的对话历史 (使用 UUID 作为键，支持玩家和控制台)
    private final Map<String, List<Map<String, String>>> conversationHistory = new HashMap<>();

    // MC专家模式的对话历史 (独立存储)
    private final Map<String, List<Map<String, String>>> mcConversationHistory = new HashMap<>();

    // 存储每个用户的 token 使用量 (UUID -> (日期 -> 使用量))
    private final Map<String, Map<String, Integer>> tokenUsage = new HashMap<>();

    // 存储每个玩家的私聊模式状态
    private final Set<String> privateModePlayers = new HashSet<>();

    // 存储每个用户的上次使用时间 (用于冷却时间检查)
    private final Map<String, Long> lastUseTime = new HashMap<>();

    /**
     * 重置每日 Token 使用量 (由主类定时任务调用)
     */
    public void resetDailyTokenUsage() {
        tokenUsage.clear();
        FanglianAI.getInstance().getLogger().info("每日 Token 使用量已重置！");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FanglianAI plugin = FanglianAI.getInstance();

        // 处理子命令
        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "clear":
                    return handleClear(sender);
                case "token":
                    return handleToken(sender);
                case "private":
                    return handlePrivate(sender);
                case "list":
                    return handleList(sender);
                case "reload":
                    return handleReload(sender);
                case "mc":
                    return handleMcMode(sender, args);
                case "translate":
                    return handleTranslate(sender, args);
                case "help":
                    return handleHelp(sender);
            }
        }

        // 基础用法检查
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("usage"));
            return true;
        }

        // 获取用户标识 (玩家用 UUID，控制台用 "console")
        String userId = getUserId(sender);

        // 执行前置检查
        if (!performPreChecks(sender, userId)) {
            return true;
        }

        // 构建问题 (普通模式)
        String question = String.join(" ", args);

        // 记录使用时间 (用于冷却时间检查，在检查通过后立即更新)
        lastUseTime.put(userId, System.currentTimeMillis());

        // 发送思考消息
        sendResponse(sender, userId, plugin.getMessage("thinking"));

        // 异步调用 API (普通模式，false = 非MC专家模式)
        processQuestion(sender, userId, question, false);

        return true;
    }

    /**
     * 获取用户标识
     */
    private String getUserId(CommandSender sender) {
        return sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "console";
    }

    /**
     * 执行前置检查（权限、冷却、Token限制）
     * @return true 表示检查通过，false 表示检查未通过
     */
    private boolean performPreChecks(CommandSender sender, String userId) {
        FanglianAI plugin = FanglianAI.getInstance();

        // 权限检查
        if (!sender.hasPermission("fanglianai.use")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return false;
        }

        // 冷却时间检查 (仅对玩家，有绕过权限的玩家跳过)
        if (sender instanceof Player && plugin.isCooldownEnabled()
            && !sender.hasPermission(plugin.getCooldownBypassPermission())) {
            if (!checkCooldown(sender, userId)) {
                return false;
            }
        }

        // 检查 token 限制 (仅对玩家)
        if (sender instanceof Player && plugin.isTokenLimitEnabled()
            && !sender.hasPermission("fanglianai.bypass.limit")) {
            if (!checkTokenLimit(sender, userId)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查冷却时间
     */
    private boolean checkCooldown(CommandSender sender, String userId) {
        FanglianAI plugin = FanglianAI.getInstance();
        Long lastTime = lastUseTime.get(userId);
        if (lastTime != null) {
            long elapsed = (System.currentTimeMillis() - lastTime) / 1000;
            int cooldownSeconds = plugin.getCooldownSeconds();
            if (elapsed < cooldownSeconds) {
                sender.sendMessage(plugin.getMessage("cooldown-wait", "seconds",
                    String.valueOf(cooldownSeconds - elapsed)));
                return false;
            }
        }
        return true;
    }

    /**
     * 检查 Token 限制
     */
    private boolean checkTokenLimit(CommandSender sender, String userId) {
        FanglianAI plugin = FanglianAI.getInstance();
        if (getTodayTokenUsage(userId) >= plugin.getDailyTokenLimit()) {
            sender.sendMessage(plugin.getMessage("token-exceeded", "limit",
                String.valueOf(plugin.getDailyTokenLimit())));
            return false;
        }
        return true;
    }

    /**
     * 处理问题并异步调用 API
     */
    private void processQuestion(CommandSender sender, String userId, String question, boolean isMcMode) {
        FanglianAI plugin = FanglianAI.getInstance();

        callOpenAIAsync(userId, question, isMcMode).thenAccept(response -> {
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendResponse(sender, userId, "§a" + response);
                }
            }.runTask(plugin);
        }).exceptionally(ex -> {
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendResponse(sender, userId,
                        plugin.getMessage("error", "error", ex.getMessage()));
                }
            }.runTask(plugin);
            return null;
        });
    }

    /**
     * 处理 MC 专家模式 (/flai mc <问题>)
     */
    private boolean handleMcMode(CommandSender sender, String[] args) {
        FanglianAI plugin = FanglianAI.getInstance();

        // 权限检查
        if (!sender.hasPermission("fanglianai.use")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        // 检查参数
        if (args.length < 2) {
            sender.sendMessage("§c用法: /flai mc <问题>");
            sender.sendMessage("§e例如: /flai mc 钻石镐怎么合成？");
            return true;
        }

        // 获取用户标识
        String userId = getUserId(sender);

        // 执行前置检查
        if (!performPreChecks(sender, userId)) {
            return true;
        }

        // 构建问题
        String question = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // 记录使用时间 (用于冷却时间检查，在检查通过后立即更新)
        lastUseTime.put(userId, System.currentTimeMillis());

        // 发送思考消息
        sendResponse(sender, userId, plugin.getMessage("thinking"));

        // 异步调用 API (MC专家模式)
        processQuestion(sender, userId, question, true);

        return true;
    }

    /**
     * 处理翻译命令 (/flai translate <内容>)
     */
    private boolean handleTranslate(CommandSender sender, String[] args) {
        FanglianAI plugin = FanglianAI.getInstance();

        // 权限检查
        if (!sender.hasPermission("fanglianai.preset")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        // 检查参数
        if (args.length < 2) {
            sender.sendMessage("§c用法: /flai translate <内容>");
            sender.sendMessage("§e例如: /flai translate Hello World");
            return true;
        }

        // 获取用户标识
        String userId = getUserId(sender);

        // 执行前置检查
        if (!performPreChecks(sender, userId)) {
            return true;
        }

        // 构建翻译内容
        String content = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // 获取翻译预设提示词并替换占位符
        String prompt = plugin.getPresetPrompt("translate");
        if (prompt == null || prompt.isEmpty()) {
            prompt = "请将以下内容翻译成中文（如果已是中文则翻译成英文）: {input}";
        }
        String question = prompt.replace("{input}", content);

        // 记录使用时间 (用于冷却时间检查，在检查通过后立即更新)
        lastUseTime.put(userId, System.currentTimeMillis());

        // 发送思考消息
        sendResponse(sender, userId, plugin.getMessage("thinking"));

        // 异步调用 API (普通模式，翻译不需要MC上下文)
        processQuestion(sender, userId, question, false);

        return true;
    }

    /**
     * 发送响应消息 (根据私聊模式决定发送方式)
     */
    private void sendResponse(CommandSender sender, String userId, String message) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            // 检查是否使用私聊模式
            boolean usePrivate = privateModePlayers.contains(userId) ||
                (FanglianAI.getInstance().isPrivateModeDefault() && !privateModePlayers.contains("public:" + userId));

            if (usePrivate) {
                player.sendMessage(message);
            } else {
                Bukkit.broadcastMessage("§b" + player.getName() + " §e问: " + message);
            }
        } else {
            sender.sendMessage(message);
        }
    }

    /**
     * 处理清除历史命令
     */
    private boolean handleClear(CommandSender sender) {
        String userId = getUserId(sender);
        conversationHistory.remove(userId);
        mcConversationHistory.remove(userId);
        sender.sendMessage(FanglianAI.getInstance().getMessage("clear-success"));
        return true;
    }

    /**
     * 处理 token 查询命令
     */
    private boolean handleToken(CommandSender sender) {
        if (!sender.hasPermission("fanglianai.token")) {
            sender.sendMessage(FanglianAI.getInstance().getMessage("no-permission"));
            return true;
        }

        FanglianAI plugin = FanglianAI.getInstance();
        String userId = getUserId(sender);

        int used = getTodayTokenUsage(userId);
        int limit = plugin.getDailyTokenLimit();

        sender.sendMessage(plugin.getMessage("token-usage",
            "used", String.valueOf(used),
            "limit", String.valueOf(limit)));
        return true;
    }

    /**
     * 处理私聊模式切换
     */
    private boolean handlePrivate(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(FanglianAI.getInstance().getMessage("console-not-allowed"));
            return true;
        }

        if (!sender.hasPermission("fanglianai.private")) {
            sender.sendMessage(FanglianAI.getInstance().getMessage("no-permission"));
            return true;
        }

        FanglianAI plugin = FanglianAI.getInstance();
        String userId = ((Player) sender).getUniqueId().toString();

        // 切换状态
        if (privateModePlayers.contains(userId)) {
            privateModePlayers.remove(userId);
            privateModePlayers.add("public:" + userId); // 标记为显式公共模式
            sender.sendMessage(plugin.getMessage("private-mode-off"));
        } else {
            privateModePlayers.remove("public:" + userId);
            privateModePlayers.add(userId);
            sender.sendMessage(plugin.getMessage("private-mode-on"));
        }
        return true;
    }

    /**
     * 处理预设列表命令
     */
    private boolean handleList(CommandSender sender) {
        sender.sendMessage("§e当前可用模式:");
        sender.sendMessage("§a/flai <问题> §7- 普通模式，通用问答");
        sender.sendMessage("§a/flai mc <问题> §7- MC专家模式，专注Minecraft问题");
        sender.sendMessage("§a/flai translate <内容> §7- 中英互译");
        return true;
    }

    /**
     * 处理重载配置命令
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("fanglianai.reload")) {
            sender.sendMessage(FanglianAI.getInstance().getMessage("no-permission"));
            return true;
        }

        FanglianAI.getInstance().reloadConfiguration();
        sender.sendMessage(FanglianAI.getInstance().getMessage("reload-success"));
        return true;
    }

    /**
     * 处理帮助命令
     */
    private boolean handleHelp(CommandSender sender) {
        FanglianAI plugin = FanglianAI.getInstance();

        sender.sendMessage(plugin.getMessage("help-header"));
        sender.sendMessage(plugin.getMessage("help-usage"));
        sender.sendMessage(plugin.getMessage("help-clear"));
        sender.sendMessage(plugin.getMessage("help-token"));
        sender.sendMessage(plugin.getMessage("help-private"));
        sender.sendMessage(plugin.getMessage("help-list"));
        sender.sendMessage(plugin.getMessage("help-mc"));
        sender.sendMessage(plugin.getMessage("help-translate"));
        sender.sendMessage(plugin.getMessage("help-reload"));
        sender.sendMessage(plugin.getMessage("help-footer"));
        return true;
    }

    /**
     * 获取今日 token 使用量
     */
    private int getTodayTokenUsage(String userId) {
        String today = LocalDate.now().toString();
        Map<String, Integer> userUsage = tokenUsage.get(userId);
        if (userUsage == null) {
            return 0;
        }
        return userUsage.getOrDefault(today, 0);
    }

    /**
     * 记录 token 使用量
     */
    private void recordTokenUsage(String userId, int tokens) {
        String today = LocalDate.now().toString();
        tokenUsage.computeIfAbsent(userId, k -> new HashMap<>())
            .merge(today, tokens, Integer::sum);
    }

    private CompletableFuture<String> callOpenAIAsync(String userId, String question, boolean isMcMode) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callOpenAI(userId, question, isMcMode);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String callOpenAI(String userId, String question, boolean isMcMode) throws IOException {
        FanglianAI plugin = FanglianAI.getInstance();

        // 根据模式选择对应的对话历史
        Map<String, List<Map<String, String>>> historyMap = isMcMode ? mcConversationHistory : conversationHistory;

        // 获取或创建用户的对话历史
        List<Map<String, String>> history = historyMap.computeIfAbsent(
            userId, k -> new ArrayList<>()
        );

        // 添加用户消息到历史
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", question);
        history.add(userMessage);

        // 构建请求消息列表
        List<Map<String, String>> messagesToSend = new ArrayList<>();

        // 添加系统提示词 (根据模式选择不同的系统提示词)
        String systemPrompt = isMcMode ? plugin.getMcSystemPromptContent() : plugin.getSystemPromptContent();
        if (plugin.isSystemPromptEnabled() && systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messagesToSend.add(systemMessage);
        }

        // 如果启用上下文，添加历史消息
        if (plugin.isContextEnabled()) {
            int maxMessages = plugin.getMaxContextMessages();
            int startIndex = Math.max(0, history.size() - maxMessages);
            messagesToSend.addAll(history.subList(startIndex, history.size()));
        } else {
            messagesToSend.add(userMessage);
        }

        // 构建 JSON 请求体
        JsonObject requestBody = new JsonObject();
        // 使用固定模型
        requestBody.addProperty("model", plugin.getApiModel());

        JsonArray messagesArray = new JsonArray();
        Gson gson = new Gson();
        for (Map<String, String> msg : messagesToSend) {
            messagesArray.add(gson.toJsonTree(msg));
        }
        requestBody.add("messages", messagesArray);

        // 使用UTF-8编码序列化JSON
        String jsonBody = gson.toJson(requestBody);

        RequestBody body = RequestBody.create(
            jsonBody,
            MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
            .url(plugin.getApiUrl())
            .post(body)
            .addHeader("Authorization", "Bearer " + plugin.getApiKey())
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(buildErrorMessage(response.code()));
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            // 提取 token 使用量
            if (jsonResponse.has("usage")) {
                JsonObject usage = jsonResponse.getAsJsonObject("usage");
                int totalTokens = usage.get("total_tokens").getAsInt();
                recordTokenUsage(userId, totalTokens);
            }

            JsonArray choices = jsonResponse.getAsJsonArray("choices");

            if (choices != null && choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                JsonObject message = choice.getAsJsonObject("message");
                if (message != null && message.has("content")) {
                    String aiResponse = message.get("content").getAsString();

                    // 将 AI 回复添加到历史
                    if (plugin.isContextEnabled()) {
                        Map<String, String> assistantMessage = new HashMap<>();
                        assistantMessage.put("role", "assistant");
                        assistantMessage.put("content", aiResponse);
                        history.add(assistantMessage);
                    }

                    return aiResponse;
                }
            }
            return "无法解析响应";
        }
    }

    /**
     * 构建友好的错误消息
     */
    private String buildErrorMessage(int code) {
        return switch (code) {
            case 429 -> "请求过于频繁，请稍后再试 (HTTP 429)";
            case 401 -> "API 密钥无效或已过期 (HTTP 401)";
            case 402 -> "API 余额不足 (HTTP 402)";
            case 500, 502, 503 -> "API 服务暂时不可用，请稍后再试 (HTTP " + code + ")";
            default -> "请求失败: HTTP " + code;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        FanglianAI plugin = FanglianAI.getInstance();
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 第一个参数：子命令
            List<String> subCommands = Arrays.asList("clear", "token", "private", "list", "reload", "help", "mc", "translate");
            String input = args[0].toLowerCase();

            // 添加匹配的子命令
            for (String subCmd : subCommands) {
                if (subCmd.startsWith(input)) {
                    completions.add(subCmd);
                }
            }
        }

        return completions;
    }
}