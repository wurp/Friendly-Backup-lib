package com.geekcommune.communication.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;

/**
 * Generic base for messages that just use a byte array to send data across.
 * @author bobbym
 */
public abstract class AbstractDataMessage extends AbstractMessage {

    protected byte[] data;

    public AbstractDataMessage(RemoteNodeHandle destination, int originNodePort) {
        super(destination, originNodePort);
    }

    public AbstractDataMessage(int transactionId, int originNodePort) {
        super(transactionId, originNodePort);
    }

    public AbstractDataMessage(int transactionId, RemoteNodeHandle destination,
            int originNodePort) {
        super(transactionId, destination, originNodePort);
    }

    public AbstractDataMessage(RemoteNodeHandle destination, int transactionId,
            int originNodePort) {
        super(destination, transactionId, originNodePort);
    }

    protected byte[] getData() {
    	return data;
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

    private void readBytes(InputStream is, byte[] data, int startIdx, int length)
            throws IOException {
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
}