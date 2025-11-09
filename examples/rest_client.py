"""
使用 requests 库的 REST API 客户端示�?
"""

import requests
import json


class MinecraftRestClient:
    """Minecraft REST API 客户�?""
    
    def __init__(self, host: str, port: int, token: str):
        self.base_url = f"http://{host}:{port}/api"
        self.headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }
        
    def get_status(self):
        """获取服务器状�?""
        response = requests.get(f"{self.base_url}/status", headers=self.headers)
        response.raise_for_status()
        return response.json()
        
    def get_players(self):
        """获取玩家信息"""
        response = requests.get(f"{self.base_url}/players", headers=self.headers)
        response.raise_for_status()
        return response.json()
        
    def send_command(self, command: str):
        """执行指令"""
        data = {"command": command}
        response = requests.post(
            f"{self.base_url}/command", 
            headers=self.headers,
            json=data
        )
        response.raise_for_status()
        return response.json()
        
    def send_message(self, message: str, sender: str = None):
        """发送消�?""
        data = {"message": message}
        if sender:
            data["sender"] = sender
        response = requests.post(
            f"{self.base_url}/message", 
            headers=self.headers,
            json=data
        )
        response.raise_for_status()
        return response.json()


def main():
    # 配置
    HOST = "localhost"
    PORT = 8766
    TOKEN = "your_secure_token_here"
    
    client = MinecraftRestClient(HOST, PORT, TOKEN)
    
    try:
        # 获取服务器状�?
        print("获取服务器状�?..")
        status = client.get_status()
        print(json.dumps(status, indent=2, ensure_ascii=False))
        print()
        
        # 获取玩家信息
        print("获取玩家信息...")
        players = client.get_players()
        print(json.dumps(players, indent=2, ensure_ascii=False))
        print()
        
        # 发送消�?
        print("发送消�?..")
        result = client.send_message("Hello from REST API!")
        print(result)
        print()
        
        # 发送消息（带发送者）
        print("发送消息（带发送者）...")
        result = client.send_message("大家好！", sender="外部用户")
        print(result)
        print()
        
        # 执行指令
        print("执行指令...")
        result = client.send_command("list")
        print(result)
        
    except requests.exceptions.RequestException as e:
        print(f"请求失败: {e}")


if __name__ == "__main__":
    main()
