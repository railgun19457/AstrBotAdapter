# 项目结构说明

```
AstrbotAdapter/
├── pom.xml                          # Maven 项目配置文件
├── README.md                        # 项目说明文档
├── QUICKSTART.md                    # 快速开始指�?
├── API.md                          # API 详细文档
├── LICENSE                         # MIT 开源协�?
├── .gitignore                      # Git 忽略文件配置
�?
├── examples/                       # 示例代码
�?  ├── python_client.py           # Python WebSocket 客户端示�?
�?  └── rest_client.py             # Python REST API 客户端示�?
�?
├── src/
�?  ├── main/
�?  �?  ├── java/
�?  �?  �?  └── com/
�?  �?  �?      └── astrbot/
�?  �?  �?          └── Adapter/
�?  �?  �?              ├── AstrbotAdapter.java           # 主插件类
�?  �?  �?              �?
�?  �?  �?              ├── command/                      # 指令�?
�?  �?  �?              �?  └── AstrbotCommand.java       # /astrbot 指令实现
�?  �?  �?              �?
�?  �?  �?              ├── listener/                     # 事件监听器包
�?  �?  �?              �?  ├── ChatListener.java        # 聊天消息监听�?
�?  �?  �?              �?  └── PlayerListener.java      # 玩家进出监听�?
�?  �?  �?              �?
�?  �?  �?              ├── manager/                      # 管理器包
�?  �?  �?              �?  ├── MessageManager.java      # 消息管理�?
�?  �?  �?              �?  └── StatusManager.java       # 状态管理器
�?  �?  �?              �?
�?  �?  �?              ├── model/                        # 数据模型�?
�?  �?  �?              �?  └── Message.java             # 消息模型
�?  �?  �?              �?
�?  �?  �?              └── server/                       # 服务器包
�?  �?  �?                  ├── AstrbotWebSocketServer.java   # WebSocket 服务器实�?
�?  �?  �?                  ├── WebSocketServer.java          # WebSocket 服务器包装类
�?  �?  �?                  └── RestApiServer.java            # REST API 服务�?
�?  �?  �?
�?  �?  └── resources/
�?  �?      ├── plugin.yml                # 插件配置文件（Bukkit 必需�?
�?  �?      └── config.yml                # 默认配置文件
�?  �?
�?  └── test/                            # 测试代码（待添加�?
�?
└── target/                              # Maven 构建输出目录
    └── AstrbotAdapter-1.0.0.jar        # 编译后的插件文件
```

## 核心组件说明

### 1. 主插件类 (AstrbotAdapter.java)
- 插件的入口点
- 负责插件的启动、停止和重载
- 管理所有子组件的生命周�?
- 提供全局访问�?

### 2. 服务器组�?(server/)

#### WebSocketServer
- **AstrbotWebSocketServer.java**: 核心 WebSocket 服务器实�?
  - 基于 Java-WebSocket �?
  - 实现 Token 认证
  - 处理客户端连接、消息和断开
  - 广播消息到所有已认证客户�?
  
- **WebSocketServer.java**: WebSocket 服务器包装类
  - 简化外部调用接�?
  - 管理服务器生命周�?

#### RestApiServer
- 基于 Java 内置�?HttpServer
- 提供 REST API 端点
- Token 认证
- JSON 响应格式

### 3. 管理器组�?(manager/)

#### MessageManager
- 处理游戏内外的消息传�?
- 格式化消�?
- 支持颜色代码转换
- 兼容 Paper �?Spigot

#### StatusManager
- 收集服务器状态信�?
- 监测 TPS、内存、玩家等
- 定期更新数据
- 提供查询接口

### 4. 监听器组�?(listener/)

#### ChatListener
- 监听玩家聊天消息
- 兼容 Paper (AsyncChatEvent) �?Spigot (AsyncPlayerChatEvent)
- 转发消息到外部服�?

#### PlayerListener
- 监听玩家加入/离开事件
- 通知外部服务

### 5. 指令组件 (command/)

#### AstrbotCommand
- 实现 `/astrbot` 指令
- 子命令：reload, status, help
- 权限检�?

### 6. 数据模型 (model/)

#### Message
- 消息数据结构
- 包含类型、玩家、内容、时间戳

## 依赖关系

```
AstrbotAdapter (主类)
├── WebSocketServer (WebSocket 服务)
├── RestApiServer (REST API 服务)
├── MessageManager (消息管理)
�?  └── WebSocketServer (发送消�?
├── StatusManager (状态管�?
├── ChatListener (聊天监听)
�?  └── MessageManager (转发消息)
├── PlayerListener (玩家监听)
�?  └── MessageManager (发送通知)
└── AstrbotCommand (指令处理)
    ├── WebSocketServer (查询状�?
    └── RestApiServer (查询状�?
```

## 通信流程

### 聊天消息流程
```
Minecraft 玩家 �?ChatListener �?MessageManager �?WebSocketServer �?AstrBot
AstrBot �?WebSocket/REST �?MessageManager �?Minecraft 服务�?�?所有玩�?
```

### 状态查询流�?
```
AstrBot �?REST API /api/status �?StatusManager �?收集数据 �?返回 JSON
AstrBot �?WebSocket status_request �?StatusManager �?返回 JSON
```

### 指令执行流程
```
AstrBot �?WebSocket/REST �?主线�?�?执行指令 �?返回结果
```

## 配置文件

### plugin.yml
Bukkit/Spigot 插件的元数据文件�?
- 插件名称、版本、作�?
- 主类路径
- API 版本
- 指令定义
- 权限定义

### config.yml
插件的运行时配置�?
- WebSocket 设置（地址、端口、Token�?
- REST API 设置（地址、端口、Token�?
- 消息格式设置
- 状态监测设�?
- 调试模式开�?

## 扩展建议

### 添加新功�?
1. 在对应的包中创建新类
2. 在主类中初始�?
3. 更新配置文件（如需要）
4. 添加相应�?WebSocket/REST 端点

### 添加新的 WebSocket 消息类型
1. �?`AstrbotWebSocketServer.java` �?`onMessage()` 方法中添加新�?case
2. 创建处理方法
3. 更新 API 文档

### 添加新的 REST 端点
1. �?`RestApiServer.java` 中创建新�?Handler �?
2. 注册端点 `server.createContext("/api/xxx", new XxxHandler())`
3. 更新 API 文档

## 性能考虑

- WebSocket 消息在异步线程中处理
- 指令执行切换到主线程（Bukkit 要求�?
- 状态更新使用独立的定时任务
- HTTP 服务器使用线程池处理请求

## 安全考虑

- Token 认证保护所有接�?
- WebSocket 连接必须先认�?
- REST API 每个请求都需�?Token
- 建议使用强随�?Token
- 可配合反向代理使�?SSL/TLS
