package com.telnet.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Main class to instantiate Telnet Server thread
 * @author sjunejo
 *
 */
public class Main {

	public static int DEFAULT_SERVER_PORT = 4444;
	public static void main(String[] args) {
	
		int server_port = DEFAULT_SERVER_PORT;
		if (args.length > 0) {
			try {
                server_port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // ignore parsing error and go with the default;
            }
		}
		
		// Start the telnet Server
		TelnetServer server = new TelnetServer(server_port);
		server.start();
		//server.acceptConnections();
		System.out.println("Telnet server started successfully on port " + server_port);
	
		// Lets give user some control from here now
		String curLine = ""; 
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(converter);
		while (true) {
			System.out.println(getMenu());
			try {
				curLine = in.readLine();
			}catch (IOException ioe) {
				System.out.println("Unable to handle the input, try again please!\n");
			}
			
			if (curLine.equals("1")) {
				System.out.println("Active connections are as follows;\n");
				System.out.println(server.listAllConnections());
			} else if (curLine.equals("2")) {
				System.out.println("|____ Please enter the connection id to disconnect: ");
				try {
					curLine = in.readLine();
					System.out.println(server.disconnectClient(curLine));
				}catch (IOException ioe) {
					System.out.println("Unable to handle the input, try again please!");
				}
			} else if (curLine.equals("3")) {
				System.out.println(server.disconnectAll());
			} else if (curLine.equals("4")) {
				System.out.println("****** This action will disconnect all connected clients and shutdown the server, do you want to continue (y/n)?");
				try {
					curLine = in.readLine();
					if (curLine.equalsIgnoreCase("y")) {
						System.out.println("......" + server.shutDown());
						break;
					} else {
						System.out.println("......Action Aborted!");
					}
				}catch (IOException ioe) {
					System.out.println("Unable to handle the input, try again please!");
				}
			} else {
				System.out.println("Option '" + curLine + "' is invalid, try again!\n");
			}
		}
	}
	
	
	/**
	 * Main Menu
	 * @return Menu as string
	 */
	private static String getMenu() {
		StringBuilder sb = new StringBuilder("\n");
		sb.append("1. List all active clients\n");
		sb.append("2. Disconnect a single client\n");
		sb.append("3. Disconnect all clients\n");
		sb.append("4. Shutdown server and quit\n");
		sb.append("> ");
		return sb.toString();
	}
		
}
