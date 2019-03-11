package hu.unideb.inf.tesla.core;

import java.util.Arrays;

public class Triplet {

	private int intervalIndex;
	private Message message;
	private byte[] MAC;

	public Triplet(int intervalIndex, Message message, byte[] MAC) {
		this.intervalIndex = intervalIndex;
		this.message = message;
		this.MAC = MAC;
	}

	public int getIntervalIndex() {
		return intervalIndex;
	}

	public void setIntervalIndex(int intervalIndex) {
		this.intervalIndex = intervalIndex;
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

	@Override
	public String toString() {
		return "Triplet{" +
				"intervalIndex=" + intervalIndex +
				", message=" + message +
				", MAC=" + Arrays.toString(MAC) +
				'}';
	}

}
