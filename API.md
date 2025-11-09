# AstrBot Adapter API 文档

## WebSocket API

### 连接地址
```
ws://{服务器IP}:{端口}
```
默认端口�?765

### 认证流程

1. 客户端连接到 WebSocket 服务�?
2. 服务器发送认证请求：
```json
{
  "type": "auth_required",
  "message": "Please authenticate with your token"
}
```

3. 客户端发送认证信息：
```json
{
  "type": "auth",
  "token": "your_secure_token_here"
}
```

4. 服务器返回认证结果：
   - 成功�?
   ```json
   {
     "type": "auth_success",
     "message": "Authentication successful"
   }
   ```
   - 失败�?
   ```json
   {
     "type": "auth_failed",
     "message": "Invalid token"
   }
   ```

### 客户端发送消�?

#### 1. 发送聊天消�?
```json
{
  "type": "chat",
  "message": "要发送的消息内容",
  "sender": "发送者名称（可选）"
}
```

**示例（不带发送者）�?*
```json
{
  "type": "chat",
  "message": "Hello from AstrBot!"
}
```

**示例（带发送者）�?*
```json
{
  "type": "chat",
  "message": "大家好！",
  "sender": "小明"
}
```

**响应�?*
```json
{
  "type": "chat_sent",
  "status": "success"
}
```

**游戏内显示效果：**
- 不带发送者：`[AstrBot] Hello from AstrBot!`
- 带发送者：`[AstrBot] <小明> 大家好！`

#### 2. 执行服务器指�?
```json
{
  "type": "command",
  "command": "指令内容（不含斜杠）"
}
```

**示例�?*
```json
{
  "type": "command",
  "command": "say Hello World"
}
```

**响应�?*
```json
{
  "type": "command_result",
  "command": "say Hello World",
  "success": true
}
```

#### 3. 请求服务器状�?
```json
{
  "type": "status_request"
}
```

**响应�?* 参见"服务器推送消�? -> "服务器状�?

#### 4. Ping
```json
{
  "type": "ping"
}
```

**响应�?*
```json
{
  "type": "pong",
  "timestamp": 1699999999999
}
```

### 服务器推送消�?

#### 1. 玩家聊天消息
```json
{
  "type": "chat",
  "player": "玩家名称",
  "message": "聊天内容",
  "timestamp": 1699999999999
}
```

#### 2. 玩家加入服务�?
```json
{
  "type": "player_join",
  "player": "玩家名称",
  "timestamp": 1699999999999
}
```

#### 3. 玩家离开服务�?
```json
{
  "type": "player_leave",
  "player": "玩家名称",
  "timestamp": 1699999999999
}
```

#### 4. 服务器状�?
```json
{
  "type": "status_response",
  "online": true,
  "version": "git-Paper-123 (MC: 1.21.1)",
  "minecraft_version": "1.21.1-R0.1-SNAPSHOT",
  "max_players": 20,
  "online_players": 5,
  "tps": [20.0, 20.0, 20.0],
  "memory": {
    "used_mb": 2048,
    "max_mb": 4096,
    "committed_mb": 2560,
    "free_mb": 2048,
    "usage_percent": 50.0
  },
  "worlds": [
    {
      "name": "world",
      "players": 5,
      "time": 6000,
      "difficulty": "NORMAL",
      "loaded_chunks": 256,
      "entities": 150
    }
  ],
  "players": ["Player1", "Player2", "Player3"]
}
```

#### 5. 错误消息
```json
{
  "type": "error",
  "message": "错误描述"
}
```

---

## REST API

### 认证

所�?REST API 请求都需要在 HTTP Header 中包含认�?Token�?

```http
Authorization: Bearer your_secure_token_here
```

或者：

```http
Authorization: your_secure_token_here
```

### 端点

#### 1. GET /api/status

获取服务器完整状态信息�?

**请求示例�?*
```http
GET /api/status HTTP/1.1
Host: localhost:8766
Authorization: Bearer your_secure_token_here
```

**响应示例�?*
```json
{
  "online": true,
  "version": "git-Paper-123 (MC: 1.21.1)",
  "minecraft_version": "1.21.1-R0.1-SNAPSHOT",
  "max_players": 20,
  "online_players": 5,
  "tps": [20.0, 20.0, 20.0],
  "memory": {
    "used_mb": 2048,
    "max_mb": 4096,
    "committed_mb": 2560,
    "free_mb": 2048,
    "usage_percent": 50.0
  },
  "worlds": [
    {
      "name": "world",
      "players": 5,
      "time": 6000,
      "difficulty": "NORMAL",
      "loaded_chunks": 256,
      "entities": 150
    }
  ],
  "players": ["Player1", "Player2", "Player3"]
}
```

#### 2. GET /api/players

获取在线玩家的详细信息�?

**请求示例�?*
```http
GET /api/players HTTP/1.1
Host: localhost:8766
Authorization: Bearer your_secure_token_here
```

**响应示例�?*
```json
{
  "online": 5,
  "max": 20,
  "list": [
    {
      "name": "Player1",
      "uuid": "00000000-0000-0000-0000-000000000000",
      "health": 20.0,
      "max_health": 20.0,
      "level": 30,
      "exp": 0.5,
      "gamemode": "SURVIVAL",
      "world": "world",
      "location": {
        "x": 100.5,
        "y": 64.0,
        "z": 200.5
      },
      "ping": 50,
      "is_op": false
    }
  ]
}
```

#### 3. POST /api/command

执行服务器控制台指令�?

**请求示例�?*
```http
POST /api/command HTTP/1.1
Host: localhost:8766
Authorization: Bearer your_secure_token_here
Content-Type: application/json

{
  "command": "say Hello World"
}
```

**响应示例�?*
```json
{
  "success": true,
  "command": "say Hello World"
}
```

**注意�?* 指令不需要包含斜�?`/`�?

#### 4. POST /api/message

向服务器发送一条消息，将在游戏内广播�?

**请求示例（不带发送者）�?*
```http
POST /api/message HTTP/1.1
Host: localhost:8766
Authorization: Bearer your_secure_token_here
Content-Type: application/json

{
  "message": "Hello from external service!"
}
```

**请求示例（带发送者）�?*
```http
POST /api/message HTTP/1.1
Host: localhost:8766
Authorization: Bearer your_secure_token_here
Content-Type: application/json

{
  "message": "大家好！",
  "sender": "外部用户"
}
```

**响应示例�?*
```json
{
  "success": true,
  "message": "Message sent"
}
```

**游戏内显示效果：**
- 不带发送者：`[AstrBot] Hello from external service!`
- 带发送者：`[AstrBot] <外部用户> 大家好！`

### 错误响应

所有错误响应都遵循以下格式�?

```json
{
  "error": "错误描述",
  "status": 错误代码
}
```

**常见错误代码�?*
- `400` - 请求参数错误
- `401` - 未认证或认证失败
- `405` - 请求方法不允�?
- `500` - 服务器内部错�?

---

## 数据类型说明

### TPS (Ticks Per Second)
服务器每秒处理的游戏刻数，正常值为 20.0。数组包含三个值：
- `tps[0]` - 1分钟平均 TPS
- `tps[1]` - 5分钟平均 TPS
- `tps[2]` - 15分钟平均 TPS

### 内存使用
- `used_mb` - 已使用内存（MB�?
- `max_mb` - 最大可用内存（MB�?
- `committed_mb` - 已分配内存（MB�?
- `free_mb` - 空闲内存（MB�?
- `usage_percent` - 内存使用百分�?

### 游戏模式
- `SURVIVAL` - 生存模式
- `CREATIVE` - 创造模�?
- `ADVENTURE` - 冒险模式
- `SPECTATOR` - 旁观者模�?

### 难度
- `PEACEFUL` - 和平
- `EASY` - 简�?
- `NORMAL` - 普�?
- `HARD` - 困难

---

## 使用示例

### Python WebSocket 客户�?
```python
import asyncio
import websockets
import json

async def connect():
    uri = "ws://localhost:8765"
    async with websockets.connect(uri) as ws:
        # 等待认证请求
        msg = await ws.recv()
        print(f"收到: {msg}")
        
        # 发送认�?
        await ws.send(json.dumps({
            "type": "auth",
            "token": "your_token"
        }))
        
        # 等待认证结果
        msg = await ws.recv()
        print(f"收到: {msg}")
        
        # 发送聊天消息（不带发送者）
        await ws.send(json.dumps({
            "type": "chat",
            "message": "Hello!"
        }))
        
        # 发送聊天消息（带发送者）
        await ws.send(json.dumps({
            "type": "chat",
            "message": "大家好！",
            "sender": "小明"
        }))

asyncio.run(connect())
```

### Python REST API 客户�?
```python
import requests

# 获取服务器状�?
response = requests.get(
    "http://localhost:8766/api/status",
    headers={"Authorization": "Bearer your_token"}
)
print(response.json())

# 发送消息（不带发送者）
response = requests.post(
    "http://localhost:8766/api/message",
    headers={"Authorization": "Bearer your_token"},
    json={"message": "Hello!"}
)
print(response.json())

# 发送消息（带发送者）
response = requests.post(
    "http://localhost:8766/api/message",
    headers={"Authorization": "Bearer your_token"},
    json={"message": "大家好！", "sender": "外部用户"}
)
print(response.json())
```

### cURL 示例
```bash
# 获取服务器状�?
curl -H "Authorization: Bearer your_token" \
     http://localhost:8766/api/status

# 执行指令
curl -X POST \
     -H "Authorization: Bearer your_token" \
     -H "Content-Type: application/json" \
     -d '{"command":"list"}' \
     http://localhost:8766/api/command

# 发送消息（不带发送者）
curl -X POST \
     -H "Authorization: Bearer your_token" \
     -H "Content-Type: application/json" \
     -d '{"message":"Hello from cURL!"}' \
     http://localhost:8766/api/message

# 发送消息（带发送者）
curl -X POST \
     -H "Authorization: Bearer your_token" \
     -H "Content-Type: application/json" \
     -d '{"message":"大家好！","sender":"外部用户"}' \
     http://localhost:8766/api/message
```

---

## 安全建议

1. **使用�?Token** - 使用至少 32 个字符的随机字符串作为认�?Token
2. **HTTPS/WSS** - 在生产环境中，建议在前面加上反向代理（如 Nginx）并启用 SSL/TLS
3. **防火�?* - 限制只有授信�?IP 地址可以访问 API 端口
4. **定期更换 Token** - 定期更换认证 Token 以提高安全�?
5. **监控日志** - 定期检查日志文件，留意可疑的认证失败记�?
