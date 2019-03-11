package hu.unideb.inf.tesla.client;

import hu.unideb.inf.tesla.core.DisclosureSchedule;
import hu.unideb.inf.tesla.core.Packet;
import hu.unideb.inf.tesla.core.Triplet;
import hu.unideb.inf.tesla.utils.TeslaUtils;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TeslaClient {

	private static final int UDP_BUFFER_SIZE = 1024;

	private String address;
	private int port;

	private DisclosureSchedule disclosureSchedule;
	private byte[][] keys;
	private int latestKeyIndex;
	private long timeDifference;
	private List<Triplet> buffer;

	private MulticastSocket multicastSocket;
	private boolean isRunning;
	private byte[] udpBuffer;

	public TeslaClient(String address, int port, long timeDifference) {

		this.address = address;
		this.port = port;
		this.timeDifference = timeDifference;

	}

	public void start() {

		// initialize
		try {
			initialize();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// run
		isRunning = true;

		while (isRunning) {
			try {
				run();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void run() throws IOException {

		// receive bytes
		byte[] receivedBytes = receiveBytes();

		// deserialize bytes to Object
		Object object = SerializationUtils.deserialize(receivedBytes);

		// cast object to DisclosureSchedule or Packet
		if (object instanceof DisclosureSchedule) {

			DisclosureSchedule disclosureSchedule = (DisclosureSchedule) object;
			handleDisclosureSchedule(disclosureSchedule);

		} else if (object instanceof Packet) {

			Packet packet = (Packet) object;
			handlePackage(packet);

		}

	}

	private void handleDisclosureSchedule(DisclosureSchedule disclosureSchedule) {

		// save the disclosure schedule for later uses
		this.disclosureSchedule = disclosureSchedule;

		// init keys
		keys = new byte[disclosureSchedule.getKeychainLength()][32];        // 32 byte = 256 bit
		for (int i = 0; i < disclosureSchedule.getKeychainLength(); i++) {
			keys[i] = null;
		}

		// get the disclosed key index
		int disclosedKeyIndex = disclosureSchedule.getIntervalIndex() - disclosureSchedule.getDisclosureDelay();

		// store the key and its index
		keys[disclosedKeyIndex] = disclosureSchedule.getKeyCommitment();
		latestKeyIndex = disclosedKeyIndex;

		// clear buffer
		buffer.clear();

	}

	private void handlePackage(Packet packet) {

		// if we didn't get a disclosure schedule yet, throw away all packets
		if (disclosureSchedule == null) {
			return;
		}

		// determine interval index
		int packetIntervalIndex = 0;

		try {
			packetIntervalIndex = TeslaUtils.determineIntervalIndex(packet.getDisclosedKey(), disclosureSchedule.getIntervalIndex(), disclosureSchedule.getKeyCommitment(), disclosureSchedule.getKeychainLength());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return;
		}

		// estimate server's interval index
		int estimatedServerIntervalIndex = TeslaUtils.estimateServerIntervalIndex(disclosureSchedule.getStartTime(), disclosureSchedule.getIntervalDuration(), disclosureSchedule.getIntervalIndex(), timeDifference);

		// if packet is not safe, throw it away
		if (!TeslaUtils.isSafePacket(estimatedServerIntervalIndex, packetIntervalIndex, disclosureSchedule.getDisclosureDelay())) {
			return;
		}

		// save important information in buffer
		Triplet tripletToSave = new Triplet(packetIntervalIndex, packet.getMessage(), packet.getMAC());
		buffer.add(tripletToSave);

		// authenticate previously stored triplets

		// get disclosed interval index
		int disclosedIntervalIndex = packetIntervalIndex - disclosureSchedule.getDisclosureDelay();

		if (disclosedIntervalIndex > latestKeyIndex) {

			// store the disclosed key and its index
			keys[disclosedIntervalIndex] = packet.getDisclosedKey();
			latestKeyIndex = disclosedIntervalIndex;

			// we can also validate the disclosed key here, but its not necessary, because if we could determine the packets index, it means that the disclosed key is valid

			// collect triplets to authenticate
			List<Triplet> tripletsToValidate = buffer.stream().filter(triplet -> triplet.getIntervalIndex() == disclosedIntervalIndex).collect(Collectors.toList());

			// validate triplets
			List<Triplet> validTriplets = tripletsToValidate.stream().filter(triplet -> {
				try {
					return TeslaUtils.isValidTriplet(triplet, packet.getDisclosedKey());
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
					return false;
				}
			}).collect(Collectors.toList());

			// remove triplets from buffer
			buffer.removeAll(tripletsToValidate);

			// handle valid messages
			handleValidTriplets(validTriplets);

		}

	}

	private void handleValidTriplets(List<Triplet> triplets) {

		System.out.println("VALID TRIPLETS:");

		for (Triplet triplet : triplets) {
			System.out.println(triplet.getMessage());
		}

		System.out.println("-----------------------------------");

	}

	private void initialize() throws IOException {

		// initialize for UDP communication
		multicastSocket = new MulticastSocket(port);
		multicastSocket.joinGroup(InetAddress.getByName(address));

		// initialize triple buffer
		buffer = new ArrayList<>();

		// initialize UDP buffer
		udpBuffer = new byte[UDP_BUFFER_SIZE];

	}

	private byte[] receiveBytes() throws IOException {

		// create datagram packet for receiving data
		DatagramPacket datagramPacket = new DatagramPacket(udpBuffer, udpBuffer.length);

		// receive packet
		multicastSocket.receive(datagramPacket);

		// get bytes
		return datagramPacket.getData();

	}

}
