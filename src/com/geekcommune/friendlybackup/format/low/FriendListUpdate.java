package com.geekcommune.friendlybackup.format.low;

import java.util.List;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.format.BaseData;
import com.geekcommune.friendlybackup.proto.Basic;

/**
 * Tells a node how to update its friends list.
 * @author bobbym
 */
public class FriendListUpdate extends BaseData<Basic.FriendListUpdate> {

    private List<RemoteNodeHandle> addList;
    private List<RemoteNodeHandle> removeList;
    private boolean removeAll;

    public FriendListUpdate(
            List<RemoteNodeHandle> addList,
            List<RemoteNodeHandle> removeList,
            boolean removeAll) {
        this.addList = addList;
        this.removeList = removeList;
        this.removeAll = removeAll;
    }

    @Override
    public Basic.FriendListUpdate toProto() {
        Basic.FriendListUpdate.Builder bldrId = Basic.FriendListUpdate.newBuilder();
        
        bldrId.setVersion(1);
        bldrId.addAllFriendsToAdd(RemoteNodeHandle.toProtoList(addList));
        bldrId.addAllFriendsToRemove(RemoteNodeHandle.toProtoList(removeList));
        bldrId.setRemoveAllCurrent(removeAll);
        
        return bldrId.build();
    }

    public static FriendListUpdate fromProto(
            Basic.FriendListUpdate proto) throws FriendlyBackupException {
        versionCheck(1, proto.getVersion(), proto);
        
        List<Basic.RemoteNodeHandle> addListProto = proto.getFriendsToAddList();
        List<RemoteNodeHandle> addList = RemoteNodeHandle.fromProtoList(addListProto);
        
        List<Basic.RemoteNodeHandle> removeListProto = proto.getFriendsToRemoveList();
        List<RemoteNodeHandle> removeList = RemoteNodeHandle.fromProtoList(removeListProto);
                
        boolean removeAll = proto.getRemoveAllCurrent();
        
        return new FriendListUpdate(addList, removeList, removeAll);
    }

    /**
     * Add all the friends in this list to the backup config.
     * @return
     */
    public List<RemoteNodeHandle> getAddList() {
        return addList;
    }

    /**
     * All the friends whose publicIdentityHandle matches on in this
     * list will be removed.  This value should be ignored if
     * isRemoveAll() returns true.
     * @return
     */
    public List<RemoteNodeHandle> getRemoveList() {
        return removeList;
    }

    /**
     * Says to remove all current friends, so the friends in getAddList() will be
     * the exact list of friends once the update is done.
     * In this case, the contents of getRemoveList() are ignored. 
     * @return
     */
    public boolean isRemoveAll() {
        return removeAll;
    }
}
