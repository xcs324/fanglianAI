# fanglianAI

一个为 Minecraft Paper 服务器提供的 AI 对话插件，让玩家可以在游戏内与 AI 交流。

## 功能特性

- 🤖 **AI 对话** - 在游戏内直接向 AI 提问
- 🎮 **MC 专家模式** - 专注于 Minecraft 问题的专业回答
- 🔄 **上下文记忆** - 支持多轮对话，AI 能记住之前的交流
- 🎯 **多模型切换** - 支持 deepseek-v3.2、deepseek-r1、qwen3-max 等多种模型
- 🌐 **中英互译** - 快速���译功能
- 🔒 **Token 限制** - 防止滥用，每日自动重置
- ⏱️ **冷却时间** - 防刷屏机制
- 💬 **私聊/公共模式** - 可选择回复可见范围
- ⚡ **异步处理** - 不阻塞服务器主线程

## 安装

1. 下载最新版本的 JAR 文件
2. 放入服务器的 `plugins/` 目录
3. 重启服务器
4. 编辑 `plugins/fanglianAI/config.yml`，配置你的 API 密钥

## 快速开始

```
# 普通模式
/flai 什么是人工智能？

# MC 专家模式
/flai mc 钻石矿在哪里能找到？

# 翻译
/flai translate Hello World
```

## 命令

| 命令 | 说明 |
|------|------|
| `/flai <问题>` | 普通模式向 AI 提问 |
| `/flai mc <问题>` | MC 专家模式提问 |
| `/flai translate <内容>` | 中英互译 |
| `/flai clear` | 清除对话历史 |
| `/flai token` | 查看今日 Token 使用量 |
| `/flai model [名称]` | 查看/切换模型 |
| `/flai models` | 查看可用模型列表 |
| `/flai private` | 切换私聊/公共模式 |
| `/flai help` | 显示帮助 |

> 命令别名：`/flai` 可简写为 `/ai`

## 配置

配置文件：`plugins/fanglianAI/config.yml`

```yaml
api:
  key: "YOUR_API_KEY_HERE"    # API 密钥（必填）
  url: "https://apis.iflow.cn/v1/chat/completions"
  model: "deepseek-v3.2"      # 默认模型
  available-models:           # 可切换的模型
    - "deepseek-v3.2"
    - "deepseek-r1"
    - "qwen3-max"

context:
  enabled: true               # 启用上下文记忆
  max-messages: 20            # 最大上下文消息数

token-limit:
  enabled: true
  daily-limit: 20000          # 每日 Token 限额

cooldown:
  enabled: true
  seconds: 15                 # 冷却时间（秒）

private-mode:
  default-enabled: true       # 默认私聊模式
```

## 权限

| 权限 | 说明 | 默认 |
|------|------|------|
| `fanglianai.use` | 使用 AI 对话 | 所有人 |
| `fanglianai.token` | 查询 Token 使用量 | 所有人 |
| `fanglianai.private` | 切换私聊模式 | 所有人 |
| `fanglianai.preset` | 使用预设指令 | 所有人 |
| `fanglianai.reload` | 重载配置 | 管理员 |
| `fanglianai.bypass.limit` | 绕过 Token 限制 | 管理员 |
| `fanglianai.bypass.cooldown` | 绕过冷却时间 | 管理员 |

## 构建

```bash
mvn clean package
```

构建产物：`target/fanglianAI-1.10.jar`

## 技术栈

- Java 17
- Paper API 1.20.4
- OkHttp 4.12.0
- Gson 2.10.1

## 许可证

MIT License

## 作者

xcs324
