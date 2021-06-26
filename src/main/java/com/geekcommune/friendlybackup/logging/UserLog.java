package com.geekcommune.friendlybackup.logging;

public abstract class UserLog {

    public abstract void logException(Exception e, String message, Object... params);

    public abstract void logError(String message, Object... params);

    public abstract void logInfo(String string, Object... params);

	protected static UserLog instance;
	
    public static UserLog instance() {
        return instance;
    }

    public static void setInstance(UserLog userLog) {
        instance = userLog;
    }
}
