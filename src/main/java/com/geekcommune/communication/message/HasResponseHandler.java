package com.geekcommune.communication.message;

import com.geekcommune.util.UnaryContinuation;

public interface HasResponseHandler extends Message {
    public UnaryContinuation<Message> getResponseHandler();
}
