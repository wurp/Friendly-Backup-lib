package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.AbstractDataMessage;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.format.low.FriendListUpdate;
import com.geekcommune.friendlybackup.proto.Basic;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Tells the receiver to modify their friends list per the FriendsListUpdate.
 * @author bobbym
 *
 */
public class UpdateFriendListMessage extends AbstractDataMessage {
    public static final int INT_TYPE = 7;
    
    public UpdateFriendListMessage(
            RemoteNodeHandle destination,
            int originNodePort,
            FriendListUpdate flu) {
        super(destination, originNodePort);
        data = flu.toProto().toByteArray();
    }
    
    public FriendListUpdate getFriendListUpdate() throws FriendlyBackupException {
        try {
            return FriendListUpdate.fromProto(Basic.FriendListUpdate.parseFrom(ByteString.copyFrom(getData())));
        } catch (InvalidProtocolBufferException e) {
            throw new FriendlyBackupException("Could not parse friend list update data", e);
        }
    }

    public int getType() {
        return INT_TYPE;
    }
}
