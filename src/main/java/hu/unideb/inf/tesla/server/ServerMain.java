package hu.unideb.inf.tesla.server;

public class ServerMain {

	public static final String ADDRESS = "225.1.2.3";
	public static final int PORT = 9999;

	public static void main(String[] args) {

		StreamingServer streamingServer = new StreamingServer(ADDRESS, PORT);

		streamingServer.start();

	}

}
