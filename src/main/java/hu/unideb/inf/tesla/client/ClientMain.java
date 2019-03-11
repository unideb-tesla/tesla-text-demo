package hu.unideb.inf.tesla.client;

public class ClientMain {

	public static final String ADDRESS = "225.1.2.3";
	public static final int PORT = 9999;
	public static final int DUMMY_DELAY = 100;

	public static void main(String[] args) {

		TeslaClient teslaClient = new TeslaClient(ADDRESS, PORT, DUMMY_DELAY);

		teslaClient.start();

	}

}
