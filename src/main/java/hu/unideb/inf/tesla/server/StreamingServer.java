package hu.unideb.inf.tesla.server;

import hu.unideb.inf.tesla.core.DisclosureSchedule;
import hu.unideb.inf.tesla.core.KeyChain;
import hu.unideb.inf.tesla.core.Message;
import hu.unideb.inf.tesla.core.Packet;
import hu.unideb.inf.tesla.utils.CryptoUtils;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

public class StreamingServer {

	public static final int INTERVAL_DURATION = 10 * 1000;    // 10 seconds
	public static final int CHAIN_LENGTH = 100;
	public static final int KEY_LENGTH_IN_BITS = 256;
	public static final int DISCLOSURE_DELAY = 2;

	private String address;
	private int port;
	private boolean isRunning;

	private KeyChain keyChain;
	private long counter;

	private MulticastSocket multicastSocket;

	public StreamingServer(String address, int port) {

		this.address = address;
		this.port = port;

		this.counter = 0;

	}

	public void start() {

		// initialize server
		try {
			initialize();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// start running
		isRunning = true;

		while (isRunning) {

			try {
				run();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				isRunning = false;
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	public void stop() {
	}

	private void run() throws NoSuchAlgorithmException, IOException {

		// check if keychain is expired
		long now = Instant.now().toEpochMilli();

		if (keyChain.isExpired(now)) {

			// regenerate keychain
			keyChain.generateKeychain();

			// send disclosure schedule
			broadcastDisclosureSchedule();

		}

		// broadcast message
		Message message = new Message(counter, "Hello world!");
		try {
			broadcastMessage(message);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		counter++;

		// sleep
		try {
			Thread.sleep(INTERVAL_DURATION);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private void initialize() throws NoSuchAlgorithmException, IOException {

		// generate keychain
		keyChain = new KeyChain(INTERVAL_DURATION, CHAIN_LENGTH, KEY_LENGTH_IN_BITS, DISCLOSURE_DELAY);
		keyChain.generateKeychain();

		// initialize UDP socket for broadcasting messages
		multicastSocket = new MulticastSocket();
		multicastSocket.setBroadcast(true);

		// send the first disclosure schedule
		broadcastDisclosureSchedule();

	}

	private void broadcastDisclosureSchedule() throws IOException {

		// collect data for a disclosure schedule
		long now = Instant.now().toEpochMilli();
		int intervalIndex = (int) Math.floor((now - keyChain.getStartOfIntervals()[0]) * 1.0 / keyChain.getIntervalDuration());
		// OLD: int keyCommitmentIndex = intervalIndex - disclosureDelay;
		int keyCommitmentIndex = intervalIndex - keyChain.getDisclosureDelay();
		// OLD: byte[] keyCommitment = (keyCommitmentIndex >= 0) ? keyChain.getKeys()[keyCommitmentIndex] : null;
		byte[] keyCommitment = keyChain.getKeys()[keyCommitmentIndex];

		// construct disclosure delay
		DisclosureSchedule disclosureSchedule = new DisclosureSchedule(keyChain.getIntervalDuration(), keyChain.getStartOfIntervals()[intervalIndex], intervalIndex, keyChain.getLength(), keyChain.getDisclosureDelay(), keyCommitment);

		// serialize disclosure schedule to byte array
		byte[] disclosureScheduleBytes = SerializationUtils.serialize(disclosureSchedule);

		// broadcast bytes
		broadcastBytes(disclosureScheduleBytes);

	}

	private void broadcastMessage(Message message) throws NoSuchAlgorithmException, InvalidKeyException, IOException {

		// TODO: if we are in the last d intervals, don't send anything!

		// collect data for constructing a package
		long now = Instant.now().toEpochMilli();
		int currentIntervalIndex = (int) Math.floor((now - keyChain.getStartOfIntervals()[0]) * 1.0 / keyChain.getIntervalDuration());
		byte[] currentKey = keyChain.getKeys()[currentIntervalIndex];
		int disclosedKeyIndex = currentIntervalIndex - keyChain.getDisclosureDelay();
		byte[] disclosedKey = keyChain.getKeys()[disclosedKeyIndex];

		// compute MAC
		byte[] messageAsBytes = SerializationUtils.serialize(message);
		byte[] mac = CryptoUtils.computeMac(messageAsBytes, currentKey);

		// construct package
		Packet packet = new Packet(message, mac, disclosedKey);

		// serialize package
		byte[] packageBytes = SerializationUtils.serialize(packet);

		// broadcast bytes
		broadcastBytes(packageBytes);

	}

	private void broadcastBytes(byte[] bytes) throws IOException {

		// create datagram packet
		DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(address), port);

		// broadcast
		multicastSocket.send(datagramPacket);

	}

}
