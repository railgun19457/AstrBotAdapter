package io.github.railgun19457.astrbotadapter.model;

public class Message {
    private String type;
    private String player;
    private String message;
    private long timestamp;
    
    public Message() {
    }
    
    public Message(String type, String player, String message) {
        this.type = type;
        this.player = player;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getPlayer() {
        return player;
    }
    
    public void setPlayer(String player) {
        this.player = player;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
