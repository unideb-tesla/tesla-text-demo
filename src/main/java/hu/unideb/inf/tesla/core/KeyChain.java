package hu.unideb.inf.tesla.core;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;

public class KeyChain implements Serializable {

	public static final String HASHING_ALGORITHM = "SHA-256";

	private long intervalDuration;
	private int length;
	private int keyLength;
	private int keyLengthInBytes;
	private int disclosureDelay;
	private long[] startOfIntervals;
	private byte[][] keys;

	public KeyChain(long intervalDuration, int length, int keyLength, int disclosureDelay) {

		this.intervalDuration = intervalDuration;
		this.length = length;
		this.keyLength = keyLength;
		this.disclosureDelay = disclosureDelay;

		// convert bits to bytes
		keyLengthInBytes = keyLength / 8;

	}

	private byte[] generateFirstKey() {

		// create random generator object and array for the key
		SecureRandom secureRandom = new SecureRandom();
		byte[] key = new byte[keyLengthInBytes];

		// generate key
		secureRandom.nextBytes(key);

		// return key
		return key;


	}

	public void generateKeychain() throws NoSuchAlgorithmException {

		// allocate array
		startOfIntervals = new long[length];

		// get the current timestamp
		long now = Instant.now().toEpochMilli();

		// calculate the start of intervals
		long shiftedStartTime = now - disclosureDelay * intervalDuration;
		for (int i = 0; i < length; i++) {
			startOfIntervals[i] = shiftedStartTime + i * intervalDuration;
		}

		// allocate keychain
		keys = new byte[length][keyLengthInBytes];

		// generate the first key
		byte[] firstKey = generateFirstKey();

		// assign the first key to the end of the chain
		keys[length - 1] = firstKey;

		// generate the rest of the chain from the preceding elements
		for (int i = length - 2; i >= 0; i--) {

			// create hasher object
			MessageDigest messageDigest = MessageDigest.getInstance(HASHING_ALGORITHM);

			// hash previous key
			messageDigest.update(keys[i + 1]);
			byte[] nextKey = messageDigest.digest();

			// save key
			keys[i] = nextKey;

		}


	}

	public boolean isExpired(long currentTime) {

		return currentTime >= startOfIntervals[length - 1] + intervalDuration;

	}

	public long getIntervalDuration() {
		return intervalDuration;
	}

	public void setIntervalDuration(long intervalDuration) {
		this.intervalDuration = intervalDuration;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getKeyLength() {
		return keyLength;
	}

	public void setKeyLength(int keyLength) {
		this.keyLength = keyLength;
	}

	public int getKeyLengthInBytes() {
		return keyLengthInBytes;
	}

	public void setKeyLengthInBytes(int keyLengthInBytes) {
		this.keyLengthInBytes = keyLengthInBytes;
	}

	public long[] getStartOfIntervals() {
		return startOfIntervals;
	}

	public void setStartOfIntervals(long[] startOfIntervals) {
		this.startOfIntervals = startOfIntervals;
	}

	public byte[][] getKeys() {
		return keys;
	}

	public void setKeys(byte[][] keys) {
		this.keys = keys;
	}

	public int getDisclosureDelay() {
		return disclosureDelay;
	}

	public void setDisclosureDelay(int disclosureDelay) {
		this.disclosureDelay = disclosureDelay;
	}

}