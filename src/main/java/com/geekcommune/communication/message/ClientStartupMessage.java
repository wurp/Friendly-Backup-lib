package com.geekcommune.communication.message;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.friendlybackup.server.format.high.ClientUpdate;
import com.geekcommune.util.UnaryContinuation;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Message to tell the server my info, so it can know if I have space to be a
 * storage node, how to contact me to see if I have good uptime, and the
 * information needed to tell someone else how to make me their friend.
 * 
 * @author wurp
 */
public class ClientStartupMessage extends AbstractDataMessage
        implements HasResponseHandler, UnaryContinuation<Message> {
    public static final Logger log = LogManager.getLogger(ClientStartupMessage.class);

    private static final int INT_TYPE = 6;

    private ConfirmationMessage response;

    private Semaphore responseSemaphore = new Semaphore(0);

    protected ClientStartupMessage(int transactionId, int originNodePort) {
        super(transactionId, originNodePort);
    }

    public ClientStartupMessage(RemoteNodeHandle rnh, int originNodePort, ClientUpdate cu) {
        super(rnh, originNodePort);
        data = cu.toProto().toByteArray();
    }

    public ClientUpdate getClientUpdate() throws FriendlyBackupException {
        try {
            return ClientUpdate.fromProto(Basic.ClientUpdate.parseFrom(ByteString.copyFrom(getData())));
        } catch (InvalidProtocolBufferException e) {
            throw new FriendlyBackupException("Could not parse client update data", e);
        }
    }

    @Override
    public int getType() {
        return INT_TYPE;
    }

    @Override
    public UnaryContinuation<Message> getResponseHandler() {
        return this;
    }

    @Override
    public void run(Message response) {
        if (response instanceof ConfirmationMessage) {
            this.response = (ConfirmationMessage) response;
            responseSemaphore.release();
        } else {
            log.warn("Found response of type {}; expected ConfirmationMessage",
                    response == null ? null : response.getClass());
        }
    }

    public synchronized boolean awaitResponse(int timeout) throws InterruptedException {
        if (response == null) {
            return responseSemaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        } else {
            return true;
        }
    }

    public synchronized ConfirmationMessage getConfirmation() {
        return response;
    }

}
