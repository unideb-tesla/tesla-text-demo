package hu.unideb.inf.tesla.core;

import java.io.Serializable;
import java.util.Arrays;

public class Packet implements Serializable {

	private Message message;
	private byte[] MAC;
	private byte[] disclosedKey;

	public Packet(Message message, byte[] MAC, byte[] disclosedKey) {
		this.message = message;
		this.MAC = MAC;
		this.disclosedKey = disclosedKey;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public byte[] getMAC() {
		return MAC;
	}

	public void setMAC(byte[] MAC) {
		this.MAC = MAC;
	}

	public byte[] getDisclosedKey() {
		return disclosedKey;
	}

	public void setDisclosedKey(byte[] disclosedKey) {
		this.disclosedKey = disclosedKey;
	}

	@Override
	public String toString() {
		return "Packet{" +
				"message=" + message +
				", MAC=" + Arrays.toString(MAC) +
				", disclosedKey=" + Arrays.toString(disclosedKey) +
				'}';
	}

}
