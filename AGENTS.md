# AGENTS.md - fanglianAI 项目上下文

## 项目概述

**fanglianAI** 是一个 Minecraft Paper 服务器插件，为玩家提供游戏内 AI 对话功能。玩家可以通过 `/flai` 命令与 AI（基于 iFlow API）进行交互。

### 核心功能
- 在游戏中使用 `/flai <问题>` 命令向 AI 提问
- 两种对话模式：普通模式和 MC 专家模式（各自独立上下文）
- 多轮对话支持，可配置上下文记忆
- 多模型切换支持（deepseek-v3.2、deepseek-r1、qwen3-max 等）
- 翻译预设指令
- Token 使用量追踪与每日限额（每日0点自动重置）
- 冷却时间防刷屏机制（默认15秒）
- 私聊/公共聊天模式切换
- 异步 API 调用，不阻塞服务器主线程
- 支持 Tab 补全

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Java 17 |
| 构建工具 | Maven |
| 服务端 API | Paper API 1.20.4 |
| HTTP 客户端 | OkHttp 4.12.0 |
| JSON 解析 | Gson 2.10.1 |
| 打包插件 | Maven Shade Plugin 3.5.0 |

## 项目结构

```
fanglianai/
├── pom.xml                    # Maven 配置
├── dependency-reduced-pom.xml # Shade 插件生成的精简 POM
├── AGENTS.md                  # 项目上下文文档
├── USER_GUIDE.md              # 用户指南
├── src/main/
│   ├── java/com/fanglian/fanglianAI/
│   │   ├── FanglianAI.java    # 插件主类，配置管理
│   │   └── FlaiCommand.java   # 命令处理器，Tab补全，API调用
│   └── resources/
│       ├── config.yml         # 用户配置文件
│       └── plugin.yml         # 插件元数据和权限定义
└── target/
    └── fanglianAI-1.10.jar    # 最终构建产物
```

## 构建与运行

### 构建项目
```bash
mvn clean package
```

构建产物位于 `target/fanglianAI-1.10.jar`

### 安装插件
1. 将构建好的 JAR 文件复制到 Paper 服务器的 `plugins/` 目录
2. 重启服务器或使用插件管理器加载
3. 首次加载会生成 `plugins/fanglianAI/config.yml`，请配置 API 密钥

## 命令详解

### 基础命令
| 命令 | 说明 | 权限 |
|------|------|------|
| `/flai <问题>` | 普通模式向 AI 提问 | `fanglianai.use` |
| `/flai mc <问题>` | MC 专家模式提问 | `fanglianai.use` |
| `/flai translate <内容>` | 中英互译 | `fanglianai.preset` |
| `/flai clear` | 清除所有对话历史 | `fanglianai.use` |
| `/flai token` | 查看今日 Token 使用量 | `fanglianai.token` |
| `/flai private` | 切换私聊/公共模式 | `fanglianai.private` |
| `/flai model [名称]` | 查看/切换当前模型 | `fanglianai.use` |
| `/flai models` | 查看可用模型列表 | `fanglianai.use` |
| `/flai list` | 查看可用命令 | `fanglianai.use` |
| `/flai reload` | 重载配置文件 | `fanglianai.reload` |
| `/flai help` | 显示帮助信息 | `fanglianai.use` |

### 命令别名
`/flai` 命令可简写为 `/ai`

## 配置说明

配置文件位于 `plugins/fanglianAI/config.yml`：

```yaml
api:
  key: "YOUR_API_KEY_HERE"           # API 密钥（必须配置）
  url: "https://apis.iflow.cn/v1/chat/completions"
  model: "deepseek-v3.2"             # 默认模型
  available-models:                  # 可切换的模型列表
    - "deepseek-v3.2"
    - "deepseek-r1"
    - "qwen3-max"
    - "qwen3-coder-plus"

context:
  enabled: true                      # 是否启用上下文记忆
  max-messages: 20                   # 最大上下文消息数

token-limit:
  enabled: true                      # 是否启用限制
  daily-limit: 20000                 # 每日限额（每日0点自动重置）

cooldown:
  enabled: true                      # 是否启用冷却
  seconds: 15                        # 冷却时间（秒）
  bypass-permission: "fanglianai.bypass.cooldown"

private-mode:
  default-enabled: true              # 默认私聊模式

system-prompt:
  enabled: true                      # 是否启用系统提示词
  content: "请用简洁清晰的中文回答用户的问题。"

mc-system-prompt:
  content: "你是 Minecraft 游戏专家..."

presets:
  translate:
    enabled: true
    prompt: "请将以下内容翻译成中文（如果已是中文则翻译成英文）: {input}"

messages:
  thinking: "§e正在思考中..."
  # ... 其他消息配置
```

## 权限系统

| 权限 | 说明 | 默认 |
|------|------|------|
| `fanglianai.use` | 基础 AI 对话功能 | 所有人 |
| `fanglianai.token` | 查询 token 使用量 | 所有人 |
| `fanglianai.private` | 切换私聊模式 | 所有人 |
| `fanglianai.preset` | 使用预设指令 | 所有人 |
| `fanglianai.reload` | 重载配置 | 管理员 |
| `fanglianai.bypass.limit` | 绕过 token 限制 | 管理员 |
| `fanglianai.bypass.cooldown` | 绕过冷却时间 | 管理员 |
| `fanglianai.admin` | 管理员权限组 | 管理员 |

## 开发约定

### 代码风格
- 包名: `com.fanglian.fanglianAI`
- 主类继承 `JavaPlugin`
- 命令处理器实现 `CommandExecutor` 和 `TabCompleter` 接口
- 使用 `BukkitRunnable` 进行主线程任务调度
- 配置访问通过主类的 getter 方法封装
- 使用常量定义配置键和权限名称

### 异步处理模式
插件采用以下模式处理异步 API 调用：
1. 使用 `CompletableFuture.supplyAsync()` 在后台线程执行 HTTP 请求
2. 使用 `BukkitRunnable.runTask()` 将结果传回主线程
3. 避免在主线程执行阻塞操作

### Maven 依赖范围
- `paper-api`: `provided` - 由服务器运行时提供
- `okhttp`, `gson`: 默认 `compile` - 打包进 JAR

### 用户标识
- 玩家: 使用 UUID 标识
- 控制台: 使用 `"console"` 字符串标识

## 关键类说明

### FanglianAI.java (主类)
- 单例模式 (`getInstance()`)
- 权限常量定义: `PERM_USE`, `PERM_TOKEN`, `PERM_PRIVATE`, `PERM_PRESET`, `PERM_RELOAD`, `PERM_BYPASS_LIMIT`, `PERM_BYPASS_COOLDOWN`
- 配置键常量定义
- 配置访问方法：
  - 基础配置: `getApiKey()`, `getApiUrl()`, `getApiModel()`, `getAvailableModels()`
  - 上下文: `isContextEnabled()`, `getMaxContextMessages()`
  - Token 限制: `isTokenLimitEnabled()`, `getDailyTokenLimit()`
  - 冷却: `isCooldownEnabled()`, `getCooldownSeconds()`, `getCooldownBypassPermission()`
  - 私聊模式: `isPrivateModeDefault()`
  - 系统提示词: `isSystemPromptEnabled()`, `getSystemPromptContent()`, `getMcSystemPromptContent()`
  - 预设: `isPresetEnabled()`, `getPresetPrompt()`, `getPresetNames()`, `getEnabledPresets()`
  - 消息: `getMessage()` (支持占位符)
- 定时任务：`startDailyResetTask()` 每日0点重置Token使用量
- 生命周期管理：`onEnable()`, `onDisable()`, `reloadConfiguration()`

### FlaiCommand.java (命令处理器)
- 实现 `CommandExecutor` 和 `TabCompleter` 接口
- 维护状态：
  - `conversationHistory` - 普通模式对话历史
  - `mcConversationHistory` - MC专家模式对话历史
  - `tokenUsage` - Token 使用量追踪
  - `privateModePlayers` - 私聊模式玩家集合
  - `lastUseTime` - 用户最后使用时间
  - `userSelectedModel` - 用户选择的模型
- 核心方法：
  - `getUserId()` - 获取用户标识
  - `performPreChecks()` - 执行前置检查（权限、冷却、Token）
  - `checkCooldown()` - 检查冷却时间
  - `checkTokenLimit()` - 检查 Token 限制
  - `processQuestion()` - 处理问题并异步调用 API
- 子命令处理：
  - `handleClear()`, `handleToken()`, `handlePrivate()`
  - `handleList()`, `handleReload()`, `handleMcMode()`
  - `handleModel()`, `handleModels()`, `handleTranslate()`, `handleHelp()`
- API 调用：
  - `callOpenAIAsync()` - 异步调用
  - `callOpenAI()` - 同步调用
  - `buildErrorMessage()` - 构建友好错误消息

## API 接口

### 请求格式
```json
{
  "model": "deepseek-v3.2",
  "messages": [
    {"role": "system", "content": "系统提示词"},
    {"role": "user", "content": "问题内容"}
  ]
}
```

### 响应格式
```json
{
  "choices": [
    {
      "message": {
        "content": "AI 回复内容"
      }
    }
  ],
  "usage": {
    "total_tokens": 150
  }
}
```

## 安全注意事项

- API 密钥从配置文件读取，不再硬编码
- 生产环境请妥善保管 `config.yml` 中的密钥
- 建议将 `config.yml` 添加到 `.gitignore`
- Token 限制可防止滥用 API 资源
- 冷却时间防止刷屏

## 版本历史

### v1.10
- 优化代码结构，提取公共检查逻辑
- 添加 translate 命令处理
- 添加权限和配置键常量
- 改进日志输出
- 使用 Java 17 switch 表达式优化错误消息构建
