#!/usr/bin/env python3
"""
AstrBot Minecraft Adapter Python 客户端示�?
演示如何通过 WebSocket 连接�?Minecraft 服务器插�?
"""

import asyncio
import websockets
import json
import logging
from typing import Optional

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger("AstrBot-Client")


class MinecraftClient:
    """Minecraft 服务�?WebSocket 客户�?""
    
    def __init__(self, host: str, port: int, token: str):
        self.host = host
        self.port = port
        self.token = token
        self.ws: Optional[websockets.WebSocketClientProtocol] = None
        self.running = False
        
    async def connect(self):
        """连接�?Minecraft 服务�?""
        uri = f"ws://{self.host}:{self.port}"
        logger.info(f"正在连接�?{uri}...")
        
        try:
            self.ws = await websockets.connect(uri)
            logger.info("连接成功�?)
            self.running = True
            
            # 启动消息处理
            await self.handle_messages()
            
        except Exception as e:
            logger.error(f"连接失败: {e}")
            
    async def handle_messages(self):
        """处理接收到的消息"""
        try:
            async for message in self.ws:
                await self.on_message(message)
        except websockets.exceptions.ConnectionClosed:
            logger.warning("连接已关�?)
            self.running = False
        except Exception as e:
            logger.error(f"消息处理错误: {e}")
            self.running = False
            
    async def on_message(self, message: str):
        """处理接收到的消息"""
        try:
            data = json.loads(message)
            msg_type = data.get("type", "")
            
            if msg_type == "auth_required":
                # 收到认证请求，发�?token
                logger.info("收到认证请求，正在发�?token...")
                await self.authenticate()
                
            elif msg_type == "auth_success":
                logger.info("�?认证成功�?)
                # 认证成功后，可以开始发送消�?
                await self.on_authenticated()
                
            elif msg_type == "auth_failed":
                logger.error("�?认证失败�?)
                await self.ws.close()
                
            elif msg_type == "chat":
                # 收到聊天消息
                player = data.get("player", "Unknown")
                msg = data.get("message", "")
                logger.info(f"💬 [{player}] {msg}")
                
            elif msg_type == "player_join":
                # 玩家加入
                player = data.get("player", "Unknown")
                logger.info(f"�?{player} 加入了游�?)
                
            elif msg_type == "player_leave":
                # 玩家离开
                player = data.get("player", "Unknown")
                logger.info(f"�?{player} 离开了游�?)
                
            elif msg_type == "status_response":
                # 服务器状态响�?
                logger.info("📊 服务器状�?")
                logger.info(f"  在线玩家: {data.get('online_players')}/{data.get('max_players')}")
                if 'tps' in data:
                    logger.info(f"  TPS: {data['tps']}")
                if 'memory' in data:
                    mem = data['memory']
                    logger.info(f"  内存: {mem.get('used_mb')}MB / {mem.get('max_mb')}MB")
                    
            elif msg_type == "command_result":
                # 指令执行结果
                cmd = data.get("command", "")
                success = data.get("success", False)
                logger.info(f"📝 指令 '{cmd}' 执行{'成功' if success else '失败'}")
                
            elif msg_type == "pong":
                logger.debug("收到 pong")
                
            elif msg_type == "error":
                error_msg = data.get("message", "Unknown error")
                logger.error(f"�?错误: {error_msg}")
                
            else:
                logger.warning(f"未知消息类型: {msg_type}")
                
        except json.JSONDecodeError:
            logger.error(f"无法解析消息: {message}")
        except Exception as e:
            logger.error(f"处理消息时出�? {e}")
            
    async def authenticate(self):
        """发送认证信�?""
        auth_msg = {
            "type": "auth",
            "token": self.token
        }
        await self.send(auth_msg)
        
    async def on_authenticated(self):
        """认证成功后的回调"""
        # 请求服务器状�?
        await self.request_status()
        
    async def send(self, data: dict):
        """发�?JSON 消息"""
        if self.ws and not self.ws.closed:
            await self.ws.send(json.dumps(data))
        else:
            logger.warning("WebSocket 未连�?)
            
    async def send_chat(self, message: str, sender: str = None):
        """发送聊天消息到 Minecraft"""
        logger.info(f"发送消�? {message}" + (f" (来自 {sender})" if sender else ""))
        payload = {
            "type": "chat",
            "message": message
        }
        if sender:
            payload["sender"] = sender
        await self.send(payload)
        
    async def send_command(self, command: str):
        """执行 Minecraft 指令"""
        logger.info(f"执行指令: {command}")
        await self.send({
            "type": "command",
            "command": command
        })
        
    async def request_status(self):
        """请求服务器状�?""
        await self.send({
            "type": "status_request"
        })
        
    async def ping(self):
        """发�?ping"""
        await self.send({
            "type": "ping"
        })
        
    async def close(self):
        """关闭连接"""
        self.running = False
        if self.ws:
            await self.ws.close()
            logger.info("连接已关�?)


async def main():
    """主函�?""
    # 配置连接信息
    HOST = "localhost"  # 服务器地址
    PORT = 8765         # WebSocket 端口
    TOKEN = "your_secure_token_here"  # 认证 token
    
    client = MinecraftClient(HOST, PORT, TOKEN)
    
    # 连接到服务器
    connect_task = asyncio.create_task(client.connect())
    
    # 等待认证完成
    await asyncio.sleep(2)
    
    # 示例：发送一些测试消�?
    if client.running:
        # 发送聊天消息（不带发送者）
        await client.send_chat("Hello from AstrBot!")
        await asyncio.sleep(1)
        
        # 发送聊天消息（带发送者）
        await client.send_chat("大家好！", sender="小明")
        await asyncio.sleep(1)
        
        # 执行指令
        await client.send_command("list")
        await asyncio.sleep(1)
        
        # 请求状�?
        await client.request_status()
        await asyncio.sleep(1)
        
        # 发�?ping
        await client.ping()
        
    # 保持连接
    try:
        await connect_task
    except KeyboardInterrupt:
        logger.info("收到中断信号，正在关�?..")
        await client.close()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n程序已退�?)
