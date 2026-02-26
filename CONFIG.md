# fanglianAI 配置文件详解

配置文件路径：`plugins/fanglianAI/config.yml`

## API 配置 (`api`)

控制 AI 服务的连接参数。

```yaml
api:
  key: "YOUR_API_KEY_HERE"
  url: "https://apis.iflow.cn/v1/chat/completions"
  model: "deepseek-v3.2"
  available-models:
    - "deepseek-v3.2"
    - "deepseek-chat"
    - "deepseek-coder"
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `key` | 字符串 | `YOUR_API_KEY_HERE` | **必填**。iFlow API 密钥，替换为您的实际密钥 |
| `url` | 字符串 | 见上 | API 端点地址，一般无需修改 |
| `model` | 字符串 | `deepseek-v3.2` | 默认使用的模型名称 |
| `available-models` | 列表 | 见上 | 玩家可切换的模型列表，每行一个模型名 |

---

## 上下文配置 (`context`)

控制多轮对话的记忆功能。

```yaml
context:
  enabled: true
  max-messages: 20
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | 布尔 | `true` | 是否启用上下文记忆。启用后 AI 能记住之前的对话 |
| `max-messages` | 整数 | `20` | 最大保留的上下文消息数（含用户消息和 AI 回复） |

---

## Token 限制配置 (`token-limit`)

控制每个玩家的 API 使用量，防止滥用。Token 使用量在每日 0 点自动重置。

```yaml
token-limit:
  enabled: true
  daily-limit: 20000
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | 布尔 | `true` | 是否启用 Token 限制 |
| `daily-limit` | 整数 | `20000` | 每个玩家每日最大 Token 使用量 |

**重置机制**：插件会在每日 0 点自动清空所有玩家的 Token 使用记录，服务器日志会显示重置提示。

**绕过权限**：拥有 `fanglianai.bypass.limit` 权限的玩家不受限制

---

## 冷却时间配置 (`cooldown`)

防止玩家频繁调用 API 刷屏。

```yaml
cooldown:
  enabled: true
  seconds: 30
  bypass-permission: "fanglianai.bypass.cooldown"
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | 布尔 | `true` | 是否启用冷却时间 |
| `seconds` | 整数 | `30` | 每次提问后需等待的秒数 |
| `bypass-permission` | 字符串 | `fanglianai.bypass.cooldown` | 绕过冷却时间的权限节点 |

---

## 私聊模式配置 (`private-mode`)

控制 AI 回复的显示方式。

```yaml
private-mode:
  default-enabled: true
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `default-enabled` | 布尔 | `true` | 默认模式。`true`=私聊（仅自己可见），`false`=公共（全服广播） |

玩家可用 `/flai private` 切换模式。

---

## 预设指令配置 (`presets`)

快捷查询模板，让玩家快速获取特定类型的信息。

### MC 相关预设

与 Minecraft 相关的预设使用 `mc` 前缀：`/flai mc <预设名> <内容>`

```yaml
presets:
  recipe:
    enabled: true
    prompt: "请告诉我 Minecraft 中 {input} 的合成配方，包括所需材料和摆放方式。"
  enchant:
    enabled: true
    prompt: "请告诉我 Minecraft 中 {input} 的最佳附魔推荐。"
```

### 其他预设

非 MC 相��预设直接使用：`/flai <预设名> <内容>`

```yaml
presets:
  translate:
    enabled: true
    prompt: "请将以下内容翻译成中文（如果已是中文则翻译成英文）: {input}"
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `<预设名>` | 节点 | 预设的唯一标识，如 `recipe`、`enchant` |
| `enabled` | 布尔 | 是否启用此预设 |
| `prompt` | 字符串 | 发送给 AI 的提示词模板，`{input}` 会被玩家输入替换 |

**使用方式**：
- MC 预设：`/flai mc <预设名> <内容>`
- 其他预设：`/flai <预设名> <内容>`

**示例**：
- `/flai mc recipe 钻石镐` → 查询钻石镐配方
- `/flai translate Hello World` → 翻译文本

**添加自定义预设**：
```yaml
presets:
  mypreset:
    enabled: true
    prompt: "这是自定义提示词：{input}"
```

---

## 系统提示词配置 (`system-prompt`)

设定 AI 的角色和行为方式。

```yaml
system-prompt:
  enabled: true
  content: "你是一个 Minecraft 游戏专家助手..."
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | 布尔 | `true` | 是否启用系统提示词 |
| `content` | 字符串 | 见配置 | AI 的角色设定，会影响所有回复的风格 |

**示例修改**：
```yaml
system-prompt:
  enabled: true
  content: "你是一个幽默的 Minecraft 向导，用轻松有趣的语气回答问题。"
```

---

## 消息配置 (`messages`)

自定义所有提示消息，支持 Minecraft 颜色代码。

```yaml
messages:
  thinking: "§e正在思考中..."
  error: "§c调用失败: {error}"
  # ... 更多消息
```

### 颜色代码

| 代码 | 颜色 |
|------|------|
| `§0` | 黑色 |
| `§1` | 深蓝 |
| `§2` | 深绿 |
| `§3` | 深青 |
| `§4` | 深红 |
| `§5` | 深紫 |
| `§6` | 金色 |
| `§7` | 灰色 |
| `§8` | 深灰 |
| `§9` | 蓝色 |
| `§a` | 绿色 |
| `§b` | 青色 |
| `§c` | 红色 |
| `§d` | 粉色 |
| `§e` | 黄色 |
| `§f` | 白色 |

### 占位符

| 占位符 | 说明 | 适用消息 |
|--------|------|----------|
| `{error}` | 错误信息 | `error` |
| `{used}` | 已用 Token | `token-usage` |
| `{limit}` | Token 限额 | `token-usage`, `token-exceeded` |
| `{seconds}` | 剩余秒数 | `cooldown-wait` |
| `{model}` | 模型名称 | `model-changed`, `model-current`, `model-not-found` |
| `{models}` | 模型列表 | `model-list` |
| `{presets}` | 预设列表 | `preset-list` |

### 完整消息列表

| 键名 | 用途 |
|------|------|
| `thinking` | AI 思考中提示 |
| `error` | 调用失败提示 |
| `usage` | 用法提示 |
| `clear-success` | 清除历史成功 |
| `token-usage` | Token 使用量显示 |
| `token-exceeded` | Token 超限提示 |
| `preset-list` | 预设列表显示 |
| `private-mode-on` | 切换到私聊模式 |
| `private-mode-off` | 切换到公共模式 |
| `reload-success` | 配置重载成功 |
| `no-permission` | 无权限提示 |
| `console-not-allowed` | 控制台禁用提示 |
| `cooldown-wait` | 冷却等待提示 |
| `model-changed` | 模型切换成功 |
| `model-current` | 当前模型显示 |
| `model-list` | 可用模型列表 |
| `model-not-found` | 模型不存在提示 |
| `help-*` | 帮助信息各部分 |

---

## 完整配置示例

```yaml
# fanglianAI 配置文件 v1.10

api:
  key: "sk-xxxxxxxxxxxxxx"
  url: "https://apis.iflow.cn/v1/chat/completions"
  model: "deepseek-v3.2"
  available-models:
    - "deepseek-v3.2"
    - "deepseek-chat"
    - "deepseek-coder"

context:
  enabled: true
  max-messages: 20

token-limit:
  enabled: true
  daily-limit: 20000
  reset-hour: 0

cooldown:
  enabled: true
  seconds: 30
  bypass-permission: "fanglianai.bypass.cooldown"

private-mode:
  default-enabled: true

system-prompt:
  enabled: true
  content: "你是一个 Minecraft 游戏专家助手，专门回答与 Minecraft 游戏相关的问题。请用简洁清晰的中文回答，必要时使用游戏内术语。如果问题与 Minecraft 无关，请礼貌地说明自己很擅长 Minecraft 然后认真回答他的问题。"

presets:
  recipe:
    enabled: true
    prompt: "请告诉我 Minecraft 中 {input} 的合成���方，包括所需材料和摆放方式。"
  enchant:
    enabled: true
    prompt: "请告诉我 Minecraft 中 {input} 的最佳附魔推荐。"
  biome:
    enabled: true
    prompt: "请告诉我 Minecraft 中 {input} 群系的特点、生成规律和稀有资源。"
  mob:
    enabled: true
    prompt: "请告诉我 Minecraft 中 {input} 生物的详细信息，包括生成条件、掉落物和行为特性。"
  command:
    enabled: true
    prompt: "请告诉我 Minecraft 命令 {input} 的详细用法和参数说明。"
  translate:
    enabled: true
    prompt: "请将以下内容翻译成中文（如果已是中文则翻译成英文）: {input}"
  explain:
    enabled: true
    prompt: "请用简单易懂的方式解释 Minecraft 中的概念: {input}"

messages:
  thinking: "§e正在思考中..."
  error: "§c调用失败: {error}"
  usage: "§c用法: /flai <问题>"
  clear-success: "§a已清除对话历史"
  token-usage: "§e今日 Token 使用情况: §a{used}§e/§c{limit}"
  token-exceeded: "§c今日 Token 使用量已达上限 ({limit})，请明天再试！"
  preset-list: "§e可用预设指令: §a{presets}"
  private-mode-on: "§a已切换到私聊模式。"
  private-mode-off: "§a已切换到公共聊天模式。"
  reload-success: "§a配置已重新加载！"
  no-permission: "§c你没有权限执行此操作。"
  console-not-allowed: "§c控制台无法使用此功能。"
  cooldown-wait: "§c请等待 {seconds} 秒后再试。"
  model-changed: "§a已切换到模型: §e{model}"
  model-current: "§e当前模型: §a{model}"
  model-list: "§e可用模型: §a{models}"
  model-not-found: "§c模型 '{model}' 不可用，使用 /flai models 查看可用模型。"
  help-header: "§e========== §b[fanglianAI 帮助] §e=========="
  help-usage: "§e/flai <问题> §7- 向 AI 提问"
  help-clear: "§e/flai clear §7- 清除对话历史"
  help-token: "§e/flai token §7- 查看 token 使用量"
  help-private: "§e/flai private §7- 切换私聊/公共模式"
  help-model: "§e/flai model [模型名] §7- 查看/切换模型"
  help-models: "§e/flai models §7- 查看可用模型列表"
  help-list: "§e/flai list §7- 查看预设指令"
  help-preset: "§e/flai <预设> <内容> §7- 使用预设指令"
  help-reload: "§e/flai reload §7- 重载配置 (管理员)"
  help-footer: "§e===================================="
```
