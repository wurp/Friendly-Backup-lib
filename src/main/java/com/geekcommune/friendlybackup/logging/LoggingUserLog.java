package com.geekcommune.friendlybackup.logging;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class LoggingUserLog extends UserLog {
    private static final Logger log = LogManager.getLogger(LoggingUserLog.class);
    private static final Logger userlog = LogManager.getLogger("UserLog");

    @Override
    public void logError(String message, Object... params) {
        userlog.error(message, params);
        log.error(message, params);
    }

    @Override
    public void logInfo(String message, Object... params) {
        userlog.info(message, params);
        log.info(message, params);
    }

    @Override
    public void logException(Exception e, String message, Object... params) {
        userlog.error(message, params, e);
        log.error(message, params, e);
    }
}
