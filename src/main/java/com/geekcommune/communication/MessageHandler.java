package com.geekcommune.communication;

import java.net.InetAddress;

import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.FriendlyBackupException;

public interface MessageHandler {

	/**
	 * Should return true if message handling is done.
	 * @param msg
	 * @return
	 */
	boolean handleMessage(Message msg, InetAddress address, boolean responseHandled) throws FriendlyBackupException;

}
