package com.telnet.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * This class is responsible for opening a port for telnet clients connections, once client is connected
 * a new TelnetClient instance will be started after checking the max connection limit, otherwise client 
 * request to connect will be declined. The active connection list will also be maintained.
 * </p>
 * @author sjunejo
 *
 */

public class TelnetServer extends Thread implements TelnetServerMBean {
    private static Logger logger = Logger.getLogger(TelnetServer.class.getName());
    
    // protect ourselves from DOS attacks
    private final static int MAX_CONNECTIONS = 5;
    private String _operatingSystem = null;
    private int _port = -1;
    private AtomicInteger _numberOfConnections = new AtomicInteger(0);
    private Map<String, TelnetClient> activeConnections = new HashMap<String, TelnetClient>();
    
    //Initialization
	private ServerSocket server = null;
	private TelnetClient _telnetClient = null;
    
    public TelnetServer() {
        // used in unit tests only
    }
    
    // For testing only
    public TelnetServer(ServerSocket sc, int port) {
    	this.server = sc;
    	_port = port;
    }
    
    /**
     * <p>Listen for client connections on all hosts on the supplied port</p>
     * @param port
     */
    public TelnetServer(int port) {
        _port = port;
    }
    
    public void run() {
        acceptConnections();
    }
    
    /**
     * This methid will open the ServerSocket on given port
     */
    protected void acceptConnections() {
        try {
            // Bind to a local port
            server = new ServerSocket (_port);

            while (true) {
            	try {
            		// Accept the next connection
            		Socket connection = server.accept();

            		// Check to see if maximum reached
            		if (getNumberOfConnections() >= MAX_CONNECTIONS) {
            			// Kill the connection
            			PrintStream pout = new PrintStream (connection.getOutputStream());
            			pout.println ("Too many users");
            			connection.close();
            			continue;
            		} else {
            			_telnetClient = new TelnetClient(this, connection, getUniqueID());
            			_telnetClient.start();
            		}
                }
            	//we throw SocketException to kill the TelnetServer Thread.
				catch (SocketException ee) { 
					break;
				}
            }
        } catch (InterruptedIOException e) {
            logger.log(Level.FINE, "This can be considered a normal exit", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to accept connections", e);
        }
    }
    
    /**
     * Current number of client connections 
     * @return Number of connections
     */
    protected int getNumberOfConnections() {
        return _numberOfConnections.intValue();
    }
    
    /**
     * This will called by client once its started successfully
     * @param clientObj TelnetClient class object
     * @return The updated number
     */
    protected int clientConnected(TelnetClient clientObj) {
    	addToActiveConnection(clientObj);
        return _numberOfConnections.addAndGet(1);
    }
    
    /**
     * This method will add the newly provided TelnetClient object in the activeConnection list
     * @param clientObj Telnetclient object
     */
    private void addToActiveConnection(TelnetClient clientObj) {
    	activeConnections.put(clientObj.getMyUniqueId(), clientObj);
    }
    
    /**
     * This method will be called to updated list of connected clients
     * @param clientObj TelnetClient object
     * @return return the updated count
     */
    protected int clientDisconnected(TelnetClient clientObj) {
    	removeFromActiveConnection(clientObj);
    	return _numberOfConnections.decrementAndGet();
    }

    /**
     * This method will add the provided TelnetClient object in the activeConnection list
     * @param clientObj Telnetclient object
     */
    private void removeFromActiveConnection(TelnetClient clientObj) {
    	activeConnections.remove(clientObj.getMyUniqueId());
    }
    
    /**
     * Return the list of active connection as String
     */
    public String listAllConnections() {
    	Iterator it = activeConnections.entrySet().iterator();
    	StringBuilder sb = new  StringBuilder();
    	sb.append("Client ID					Client Object\n");
    	sb.append("=========					=============\n");
	    while (it.hasNext()) {
	    	Map.Entry pairs = (Map.Entry) it.next();
	    	sb.append(pairs.getKey()).append("		").append(pairs.getValue().toString()).append("\n");
	    }
	    sb.append("Active Connections = ").append(getNumberOfConnections());
	    sb.append(", ");
	    sb.append("Max Connections = ").append(MAX_CONNECTIONS);
	    sb.append("\n");
	    return sb.toString();
	}
	
    /**
     * This method will disconnect the client for which the id is provided
     * @param clientId Unique clientId
     * @return 'Success' if everything is OK, otherwise will return the error message
     */
	public String disconnectClient(String clientId) {
		if (clientId != null) {
			TelnetClient clientToKill = activeConnections.get(clientId);
			if (clientToKill != null) {
				try {
					clientToKill.destroyTelnetClientThread();
					//clientDisconnected(clientToKill);
				} catch (SocketException se) {
					logger.log(Level.SEVERE, "Failed to disconnect client " + clientId + " - " + se.getMessage());
					return "Failed";
				}
				return "Success";
			} else {
				return "Invalid client id provided";
			}
		} else {
			return "Client id is mandatory input";
		}
	}

	/**
	 * This method will disconnect all active client connections and shutdown the server
	 * @return 'Success' if everything is OK, otherwise error message be returned 
	 */
	public String disconnectAll() {
		Iterator it = activeConnections.entrySet().iterator();
		while (it.hasNext()) {
	    	Map.Entry pairs = (Map.Entry) it.next();
	    	if (disconnectClient((String) pairs.getKey()).equals("Failed")) {
	    		return "Failed";
	    	}
		}
		return "Success";
	}

	/**
	 * This method will shutdown the Telnet Server thread
	 */
	public String shutDown() {
		disconnectAll();
		try {
			server.close();
		} catch (IOException ioe) {
			logger.log(Level.SEVERE, "Failed to stop server - " + ioe.getMessage());
			return "Failed";
		}
		return "Goodbye";
	}
    
	/**
	 * This method will generate the unique ID 
	 * @return Unique ID as String
	 */
	private String getUniqueID() {
		return (UUID.randomUUID().toString());
	}

	/**
	 * This method will return the current OS on which the server is running
	 * @return OS value as String
	 */
	public String getOS() {
		if (_operatingSystem == null) {
			_operatingSystem = System.getProperty("os.name");
		}
		return _operatingSystem;
    }

	/**
	 * Return the Max Client Connections allowed value
	 * @return Max value as int
	 */
	public int getMaxConnections() {
		return MAX_CONNECTIONS;
	}
}
