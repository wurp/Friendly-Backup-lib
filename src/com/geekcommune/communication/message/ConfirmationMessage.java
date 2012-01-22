package com.geekcommune.communication.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;

/**
 * Send a reply telling if the operation succeeded, and if not, describe the failure.
 * @author wurp
 *
 */
public class ConfirmationMessage extends AbstractMessage {

	private static final int INT_TYPE = 2;
	
	private String errorMessage;

	public ConfirmationMessage(RemoteNodeHandle destination, int transactionId, int originNodePort) {
        super(transactionId, destination, originNodePort);
    }

	@Override
	public int getType() {
		return INT_TYPE;
	}

	public void setOK() {
		errorMessage = null;
	}
	
	public boolean isOK() {
		return errorMessage == null;
	}
	
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	@Override
	protected void internalRead(DataInputStream is) throws IOException,
			FriendlyBackupException {
		boolean isOK = is.readBoolean();
		if( !isOK ) {
			errorMessage = is.readUTF();
		}
	}

	@Override
	protected void internalWrite(DataOutputStream os) throws IOException,
			FriendlyBackupException {
		if( errorMessage == null ) {
			os.writeBoolean(true);
		} else {
			os.writeBoolean(false);
			os.writeUTF(errorMessage);
		}
	}

}
