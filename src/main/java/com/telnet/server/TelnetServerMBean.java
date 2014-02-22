package com.telnet.server;

public interface TelnetServerMBean {
	
	/**
	 * List all connections
	 */
	String listAllConnections();
	
	/**
	 * Will Disconnect a single active connection by using client id
	 * @param clientId The id of the client to disconnect
	 * @return Response of the operation performed,
	 */
	String disconnectClient(String clientId);
	
	/**
	 * This will close all client connections
	 * @return Response of the operation performed
	 */
	String disconnectAll();
	
	/**
	 * This method can be invoke to stop the Telnet Server
	 * @return Response of the operation performed
	 */
	String shutDown();
}
