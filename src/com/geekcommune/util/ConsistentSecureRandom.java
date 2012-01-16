package com.geekcommune.util;

import java.security.Provider;
import java.security.SecureRandom;

public class ConsistentSecureRandom extends SecureRandom {
	public ConsistentSecureRandom(byte[] seed) {
		setSeed(seed);
	}

	public void setSeed(byte[] seed) {
		//TODO
	}
	
	public void setSeed(long seed) {
		//use the byte[] version of setSeed, sending the contents of the long
		byte[] seed2 = new byte[8];
		for(int i = 0; i < seed2.length; ++i) {
			seed2[i] = (byte) (seed & 0xff);
			seed = seed >> 8;
		}
		
		setSeed(seed2);
	}
	
	public void 	nextBytes(byte[] bytes) {
		
	}
	
    public String getAlgorithm() {
    	return null;
    }
}
