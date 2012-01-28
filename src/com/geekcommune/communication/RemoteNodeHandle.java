package com.geekcommune.communication;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.format.BaseData;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.identity.PublicIdentityHandle;

public class RemoteNodeHandle extends BaseData<Basic.RemoteNodeHandle> {

    private static final String CONNECT_STRING_SEP = ":";

    private String name;
    private String email;
    private InetAddress address;
    private int port;
    private PublicIdentityHandle publicIdentity;

    public RemoteNodeHandle(String name, String email, String connectString, PublicIdentityHandle publicIdentity) throws FriendlyBackupException {
        this.name = name;
        this.email = email;

        String[] cstringPart = connectString.split(CONNECT_STRING_SEP);

        try {
			this.address = InetAddress.getByName(cstringPart[0]);
		} catch (UnknownHostException e) {
            throw new FriendlyBackupException("Please double check the host name in " + cstringPart[0], e);
		}

		this.port = Integer.parseInt(cstringPart[1]);
		
		this.publicIdentity = publicIdentity;
    }

    public Basic.RemoteNodeHandle toProto() {
        Basic.RemoteNodeHandle.Builder bldr = Basic.RemoteNodeHandle.newBuilder();
        bldr.setConnectString(getConnectString());
        bldr.setEmail(email);
        bldr.setName(name);
        bldr.setPublicIdentity(publicIdentity.toProto());
        bldr.setVersion(1);
        
        return bldr.build();
    }

    public String getConnectString() {
        return "" + address.getHostAddress() + CONNECT_STRING_SEP + port;
    }

    @Override
    public boolean equals(Object obj) {
        if( obj instanceof RemoteNodeHandle ) {
            RemoteNodeHandle rhs = (RemoteNodeHandle) obj;
            return name.equals(rhs.name) && email.equals(rhs.email) && getConnectString().equals(rhs.getConnectString()) && publicIdentity.equals(rhs.publicIdentity);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (name + "~" + email + "~" + getConnectString() + "~" + publicIdentity.getHandleString()).hashCode();
    }
    
    @Override
    public String toString() {
        return "RemoteNodeHandle(" + name + ", " + email + ", " + getConnectString() + ")"; 
    }
    
    public static RemoteNodeHandle fromProto(Basic.RemoteNodeHandle proto) throws FriendlyBackupException {
        versionCheck(1, proto.getVersion(), proto);
        
        String name = proto.getName();
        String email = proto.getEmail();
        String connectString = proto.getConnectString();
        PublicIdentityHandle publicIdentity =
                PublicIdentityHandle.fromProto(proto.getPublicIdentity());
        
        return new RemoteNodeHandle(name, email, connectString, publicIdentity);
    }

    public static List<RemoteNodeHandle> fromProtoList(List<Basic.RemoteNodeHandle> protoList) throws FriendlyBackupException {
        List<RemoteNodeHandle> retval = new ArrayList<RemoteNodeHandle>();
        for(Basic.RemoteNodeHandle rnh : protoList) {
            retval.add(RemoteNodeHandle.fromProto(rnh));
        }
        
        return retval;
    }

    public static List<Basic.RemoteNodeHandle> toProtoList(
            List<RemoteNodeHandle> rnhList) {
        List<Basic.RemoteNodeHandle> retval = new ArrayList<Basic.RemoteNodeHandle>();
        for(RemoteNodeHandle rnh : rnhList) {
            retval.add(rnh.toProto());
        }
        
        return retval;
    }
    
    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public PublicIdentityHandle getPublicIdentity() {
        return publicIdentity;
    }

    public void setPublicIdentity(PublicIdentityHandle publicIdentity) {
        this.publicIdentity = publicIdentity;
    }
}
