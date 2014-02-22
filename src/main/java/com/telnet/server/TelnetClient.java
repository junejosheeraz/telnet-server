package com.telnet.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>This class is responsible for receiving input from a unique 
 * user on the end of a socket connection.  The user commands will be
 * validated and performed.</p>
 * @author sjunejo
 */

public class TelnetClient extends Thread {
    private static Logger logger = Logger.getLogger(TelnetClient.class.getName());
    private final static String PROMPT = "> ";
    private String _myUniqueId = null;
    private String _currentWorkingDirectory = null;
    
    private TelnetServer _telnetServer = null;
    private Socket _client = null;
    private InputStream _in = null;
    private OutputStream _os = null;
    private boolean _stop = false;

    protected TelnetClient() {
        // used in unit tests
    }
    
    protected TelnetClient(TelnetServer tc) {
        // used in unit tests
    	_telnetServer = tc;
    }
    
    public TelnetClient(TelnetServer tc, Socket client, String uniqueID) {
    	_telnetServer = tc;
        _client = client;
        _myUniqueId = uniqueID; 
    }

    public void run() {
        try {
        	_telnetServer.clientConnected(this);
            _in = _client.getInputStream();
            _os = _client.getOutputStream();
            
            BufferedReader r = new BufferedReader(new InputStreamReader(_in));
            PrintStream pso = new PrintStream(_os);
            pso.println("Welcome to the Test Telnet Server, following commands are available...\n");
            pso.println(getHelpOutput());
            pso.print(getPrompt());
            pso.flush();
            
            String cmd = null;
            while (!_stop && (cmd = r.readLine()) != null) {
            	pso.println(performTelnetCommand(cmd));
                pso.print(getPrompt());
                pso.flush();
            }
        } catch (SocketException se) {
			return;
        } catch (IOException e) {
            logger.log(Level.WARNING, "An error occurred while handling client input", e);
        } finally {
            _telnetServer.clientDisconnected(this);
        }

    }
    
    /** The method is used to kill the TelnetClient Thread
     * <li> Basically the BufferedReader class methods are  synchronized and blocking IO operations.
     * <li> To overcome this inherent Blocking feature while the execution of thread , we need to explicitly 
     * close the socket which are in use, to throw an SocketException to accomplish the Exit of Thread.run()  
     */
	public void destroyTelnetClientThread() throws SocketException  {
		try {
			_client.close();
		} catch (IOException e) {
			//Actually IOException , but we throw as SocketException
			throw new SocketException();
		}
	}
    
	/**
	 * This method will parse the user input and returns an String[] as command
	 * @param cmd User input as single String
	 * @return String[]
	 */
	protected String[] getCommands(String cmd) {
        if (cmd != null) {
            // remove leading & trailing whitespace
            cmd.trim();
            // strip out undesirable spaces e.g. "1 2 3  4    5" "1 2 3 4 5"
            cmd = cmd.replaceAll("[ ]{2,}", " ");
            
            String[] cmds = cmd.split(" ");
            if (cmds.length > 0) {
                return cmds;
            }
        }
        return null;
    }
    
	/**
	 * This method will run the actual command and return the response
	 * @param cmd Command to execute
	 * @return Response as String
	 */
    public String performTelnetCommand(String cmd) {
        String[] cmds = getCommands(cmd);
        if (cmds != null && cmds.length > 0) {
            String operation = cmds[0];
            if ("ls".equalsIgnoreCase(operation) || "dir".equalsIgnoreCase(operation)) {
            	if (cmds.length > 1) {
            		String dirStr = cmds[1];
            		return listAllFiles(dirStr);
            	}
            	return listAllFiles();
            } else if ("cd".equalsIgnoreCase(operation)) {
                if (cmds.length > 1) {
                    String dirStr = cmds[1];
                    return changeCurrentWorkingDir(dirStr);
                }
            } else if ("pwd".equalsIgnoreCase(operation)) {
                return getCurrentWorkingDirectory();
            } else if ("mkdir".equalsIgnoreCase(operation)) {
            	String dirName, result = "";
        		// Following is to support multiple directories provided as arguments
        		for (int i = 1; i < cmds.length; i++) {
        			dirName = cmds[i];
        			result = createNewDir(dirName);
        			if (! result.equals("")) {
        				return result;
        			}
        		}
        		return "";
            } else if ("?".equals(operation)) {
                return getHelpOutput();
            } else if ("quit".equals(operation)) {
                try {
                    _client.close();
                    _stop = true;
                } catch (IOException e) {
                    logger.log(Level.WARNING, "An error occurred while closing client connection", e);
                }
                return "Connection closed";
            } else {
            	return "Supplied command is not supported - Enter '?' for list of valid commands";
            }
        }
        return "";
    }
    
    /**
     * List all files in current working directory
     * @return List of files as String
     */
    protected String listAllFiles() {
    	return listAllFiles(getCurrentWorkingDirectory());
    }
    
    /**
     * This method will return the list of all files in dir provided
     * @return
     */
    protected String listAllFiles(String dirStr) {
    	File f = null;
    	if (getCurrentWorkingDirectory().equals(dirStr)) {
    		f = new File(getCurrentWorkingDirectory());
    	} else {
    		f = new File(getCanonicalPath(dirStr));
    	}
    	if (f.exists() && f.isDirectory()) {
    		File[] files = f.listFiles();
			StringBuilder sb = new StringBuilder();
			for (File file : files) {
				if (file.isDirectory()) {
					sb.append("<DIR>__ ");
				} else {
					sb.append("_______ ");
				}
				sb.append(file.getName()).append("\n");
			}
			return sb.toString();
		} else {
			return dirStr + " - either it is not a directory or it does not exist";
		}
    }
    
    /**
     * This method will change the users current directory
     * @param dirStr
     * @return
     */
    protected String changeCurrentWorkingDir(String dirStr) {
    	return setCurrentWorkingDirectory(dirStr);
    }
    
    /**
     * This method will set the current working directory for user sessions
     * @param newWorkDir Required new working directory
     * @return Updated current working directory
     */
    protected String setCurrentWorkingDirectory(String newWorkDir) {
    	String canonicalPath = getCanonicalPath(newWorkDir);
    	File file = new File(canonicalPath);
    	if (!file.exists()) {
    		return newWorkDir + " - does not exist";
    	} else if (! file.isDirectory()) {
    		return newWorkDir + " - is not a directory";
    	}
    	_currentWorkingDirectory = canonicalPath;
    	return "";
    }
    
    /**
     * Returns the prompt for user
     * @return Prompt as string to pass to connected client
     */
    protected String getPrompt() {
    	return getCurrentWorkingDirectory() + PROMPT;
    }
    
    /**
     * Returns the users current working directory
     * @return
     */
    protected String getCurrentWorkingDirectory() {
    	if (_currentWorkingDirectory == null) {
    		_currentWorkingDirectory = System.getProperty("user.dir");
    	}
    	return _currentWorkingDirectory;
    }
    
    /**
     * This method will return the help text
     * @return Help menu as string
     */
    
    private String getHelpOutput() {
    	StringBuilder sb = new StringBuilder("?   - Display this help menu.\r\n");
    	if (isWindows()) {
    		sb.append("dir - List the current working directory.\r\n");
    	} else {
    		sb.append("ls  - List the current working directory.\r\n");
    	}
    	sb.append("cd <DIRECTORY_NAME> - Change the current working directory to the provided arguments.\r\n");
    	sb.append("pwd - Display the current working directory.\r\n");
    	sb.append("mkdir <DIRECTORY_NAME> - Create a directory.\r\n");
    	sb.append("quit - To disconnect.\n\n");
        return sb.toString();
    }
   
    /**
     * Check if we are running on Windows
     * @return
     */
    private boolean isWindows() {
    	return _telnetServer.getOS().startsWith("Windows");
    }
    
    /**
     * Method to run mkdir command    
     * @param newDir
     * @return
     */
    protected String createNewDir(String newDir) {
    	boolean opStatus = new File(getCanonicalPath(newDir)).mkdirs();
    	if (!opStatus) {
    		return "Failed to create directory '" + newDir + "'";
    	}
    	return "";
    }
    
    /**
     * Unique ID of this thread if required
     * @return
     */
    public String getMyUniqueId() {
    	return _myUniqueId;
    }
    
    /**
     * This method will parse the user input and return the new working directory
     * @param dirStr New working directory
     * @return parsed working directory
     */
    private String getCanonicalPath(String dirStr) {
    	File f = null;
    	// First check with root conditions for both platform
    	if ( (isWindows() && dirStr.startsWith("\\")) ||
    			(isWindows() && dirStr.matches("[A-Za-z]:[\\\\/].*")) ||
    			(!isWindows() && dirStr.startsWith("/")) ){
    		f = new File(dirStr);
    	} else {
    		// This means user has provided a relative path so start to parse from current working directory
    		f = new File(_currentWorkingDirectory, dirStr);
    	}
    	try {
    		return f.getCanonicalPath();
    	} catch (IOException ioe) {
    		return "";
    	}
    }
}
