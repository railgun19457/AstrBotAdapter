# AstrBot Adaptor

一个用于连接 Minecraft 服务器和 AstrBot 的插件，支持消息互通、服务器状态监测和远程指令执行。

**作者**: [railgun19457](https://github.com/railgun19457)  
**仓库**: [https://github.com/railgun19457/AstrBotAdaptor](https://github.com/railgun19457/AstrBotAdaptor)

## 特性

- ✅ **消息互通** - 服务器聊天消息转发至 AstrBot，AstrBot 也可以发送消息至服务器，支持发送者信息
- ✅ **WebSocket 服务器** - 使用 WebSocket 实现实时双向通信，服务器作为 WebSocket 服务端
- ✅ **REST API** - 提供 HTTP 接口查询服务器状态和执行操作
- ✅ **Token 鉴权** - 安全的 Token 认证机制
- ✅ **服务器状态监测** - 实时监测玩家信息、TPS、内存使用等
- ✅ **远程指令执行** - 通过外部接口执行服务器指令
- ✅ **玩家事件通知** - 玩家加入/离开通知

## 兼容性

- **服务器核心**: Paper, Spigot, Leaf 等主流插件服核心
- **Minecraft 版本**: 1.20.x, 1.21.x
- **Java 版本**: 17+

## 快速开始

### 1. 安装

1. 从 [Releases](https://github.com/railgun19457/AstrBotAdaptor/releases) 下载最新版本的 jar 文件
2. 将文件放入服务器的 `plugins` 目录
3. 重启服务器
4. 插件会自动生成配置文件 `plugins/AstrbotAdaptor/config.yml`

### 2. 配置

首次启动时，插件会自动检测并生成安全的随机 Token。

编辑 `plugins/AstrbotAdaptor/config.yml`：

```yaml
websocket:
  enabled: true
  host: "0.0.0.0"
  port: 8765
  token: "自动生成的token"  # 插件会自动生成

rest-api:
  enabled: true
  host: "0.0.0.0"
  port: 8766
  token: "自动生成的token"  # 插件会自动生成
```

**✨ 自动生成 Token**: 首次启动插件时，如果配置文件中的 token 为空或使用默认值，插件会自动生成加密安全的随机 Token 并保存到配置文件中。生成的 Token 会在控制台显示，请妥善保管！

**🔄 手动修改 Token**: 如果需要自定义 Token，可以手动修改配置文件中的 token 值。

**生成安全 Token 的方法（如需手动生成）：**

```bash
# PowerShell
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | % {[char]$_})

# Linux/Mac
openssl rand -base64 32

# Python
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

保存配置后，在游戏内或控制台执行：
```
/astrbot reload
```

### 3. 连接测试

#### WebSocket 连接
```
ws://服务器IP:8765
```

#### REST API
```
http://服务器IP:8766/api/status
```

## API 文档

完整的 API 使用说明请查看：

📖 **[API 文档](API.md)** - WebSocket 和 REST API 完整说明

包含内容：
- WebSocket 认证和消息格式
- REST API 端点详细说明
- 请求/响应示例
- Python、cURL 使用示例

## 游戏内指令

- `/astrbot reload` - 重载插件配置
- `/astrbot status` - 显示服务器和插件状态
- `/astrbot help` - 显示帮助信息

权限：`astrbot.admin` (默认: OP)


## 文档

- 📖 [API 文档](API.md) - 完整 API 说明
- 🏗️ [项目结构](STRUCTURE.md) - 代码结构和扩展指南

## 编译

需要 Java 17 和 Maven 3.6+：

```bash
mvn clean package
```

编译后的 jar 文件位于 `target/AstrbotAdaptor-1.0.0.jar`

## 开源协议

MIT License - 详见 [LICENSE](LICENSE)

## 贡献

欢迎提交 Issue 和 Pull Request！

## 支持

- 🐛 [报告问题](https://github.com/railgun19457/AstrBotAdaptor/issues)
- 💡 [功能建议](https://github.com/railgun19457/AstrBotAdaptor/issues)
- 📧 联系作者：通过 GitHub Issue
