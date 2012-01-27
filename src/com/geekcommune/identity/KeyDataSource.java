package com.geekcommune.identity;

import java.util.Properties;

public interface KeyDataSource {

    char[] getPassphrase();

    String getIdentity();

    void clearPassphrase();

	void initFromProps(String propNamePrefix, Properties props);
}
