package basketcasey.com.androidsecurenote.cryptography;

import java.io.Serializable;

import javax.crypto.SecretKey;

public class CryptoStore implements Serializable{

	private static final long serialVersionUID = 1L;
	private SecretKey key = null;
	
	public void setKey(SecretKey key) {
		this.key = key;
	}
	
	public SecretKey getKey() {
		return this.key;
	}
}
