package com.geekcommune.communication.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.friendlybackup.server.format.high.ClientUpdate;
import com.geekcommune.util.UnaryContinuation;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Message to tell the server my info.
 * @author wurp
 */
public class ClientStartupMessage extends AbstractMessage implements HasResponseHandler, UnaryContinuation<Message> {
    public static final Logger log = Logger.getLogger(ClientStartupMessage.class);
    
	private static final int INT_TYPE = 6;
	
	private byte[] data;

	private ConfirmationMessage response;

	private Semaphore responseSemaphore = new Semaphore(1);

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
			throw new FriendlyBackupException("Could not retrieve client update data", e);
		}
	}

	private byte[] getData() {
		return data;
	}

	@Override
	public int getType() {
		return INT_TYPE;
	}

	@Override
	protected void internalRead(DataInputStream is) throws IOException,
			FriendlyBackupException {
        int len = is.readInt();
        
        if( len != 0 ) {
            data = new byte[len];
            readBytes(is, data, 0, data.length);
//            if( data.length != bytesRead ) {
//                int remaining = is.read();
//                throw new RuntimeException("Could not read all " + data.length + " bytes, found " + bytesRead + ", found " + remaining + " when looking for more data");
//            }
        }
	}

    private void readBytes(InputStream is, byte[] data, int startIdx, int length) throws IOException {
        int total = 0;
        while( total < length ) {
            int lastRead = is.read(data, startIdx + total, length - total);
            if( lastRead == -1 ) {
                throw new RuntimeException("Could not read all " + length + " bytes, found " + total + ", before hitting end of stream");
            }
            total += lastRead;
        }
    }

	@Override
	protected void internalWrite(DataOutputStream os) throws IOException,
			FriendlyBackupException {
        byte[] data = getData();
        os.writeInt(data.length);
        os.write(data);
	}

	@Override
	public UnaryContinuation<Message> getResponseHandler() {
		return this;
	}

	@Override
	public void run(Message response) {
		if( response instanceof ConfirmationMessage ) {
			this.response = (ConfirmationMessage) response;
			responseSemaphore.release();
		} else {
			log.warn("Found response of type " + 
					(response == null ? null : response.getClass()) +
					"; expected ConfirmationMessage");
		}
	}

	public synchronized void awaitResponse(int timeout) throws InterruptedException {
		if( response == null ) {
			responseSemaphore.acquire(timeout);
		}
	}

	public synchronized ConfirmationMessage getConfirmation() {
		return response;
	}

}
