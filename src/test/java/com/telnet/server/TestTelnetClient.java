package com.telnet.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import org.junit.Test;

public class TestTelnetClient {
	
	@Test
	public void testGetCommands() {
		TelnetClient client = new TelnetClient();
		String[] args = client.getCommands("ls      test  something   else");
		assertEquals("Number of arguments", 4, args.length);
		assertEquals("Argument No 3 Should be", "something", args[2]);
	}

	@Test
	public void testInvalidCommands() {
		TelnetClient client = new TelnetClient();
		String reply = client.performTelnetCommand(null);
		assertEquals ("We should recieve empty string", "", reply);
		
		reply = client.performTelnetCommand("");
		assertEquals ("Error message should be returned", "Supplied command is not supported - Enter '?' for list of valid commands", reply);
	}
	
	@Test
	public void testListCommand() {
		TelnetServer mockTC = mock(TelnetServer.class);
		TelnetClient client = new TelnetClient(mockTC);;
		String operatingsystem = System.getProperty("os.name");
		
		// All the tests for Windows platform
		if (operatingsystem.startsWith("Windows")) {
			when(mockTC.getOS()).thenReturn("Windows");
					
			String reply = client.performTelnetCommand("ls somethingWhichIsNotExpected");
			assertEquals("Error message should be returned", "somethingWhichIsNotExpected - either it is not a directory or it does not exist", reply);
			
			reply = client.performTelnetCommand("dir");
			assertFalse("Reply should have something", reply.equals(""));
			
			reply = client.performTelnetCommand("dir /");
			assertFalse("Reply should have something", reply.equals(""));
			
			reply = client.performTelnetCommand("dir ..");
			assertFalse("Reply should have something", reply.equals(""));
			
			reply = client.performTelnetCommand("dir \\");
			assertFalse("Reply should have something", reply.equals(""));
			
			reply = client.performTelnetCommand("dir C:\\");
			assertFalse("Reply should have something", reply.equals(""));
		} else {
			when(mockTC.getOS()).thenReturn("Unix");
			
			String reply = client.performTelnetCommand("ls somethingWhichIsNotExpected");
			assertEquals("Error message should be returned", "somethingWhichIsNotExpected - either it is not a directory or it does not exist", reply);
			
			reply = client.performTelnetCommand("ls");
			assertFalse("Reply should have something", reply.equals(""));
			
			reply = client.performTelnetCommand("ls /");
			assertFalse("Reply should have something", reply.equals(""));
			
			reply = client.performTelnetCommand("dir ..");
			assertFalse("Reply should have something", reply.equals(""));
		}
	}
	
	@Test
	public void testCDCommand() {
		
		TelnetServer mockTC = mock(TelnetServer.class);
		TelnetClient client = new TelnetClient(mockTC);
		String operatingsystem = System.getProperty("os.name");
		
		// All the tests for Windows platform
		if (operatingsystem.startsWith("Windows")) {
			when(mockTC.getOS()).thenReturn("Windows");
			
			String reply = client.performTelnetCommand("cd somethingWhichIsNotExpected");
			assertEquals("Error message should be returned", "somethingWhichIsNotExpected - does not exist", reply);
			
			String expected = "";
			reply = client.performTelnetCommand("cd");
			assertEquals("Reply should be empty as we no going to perform any action", expected, reply);
			
			expected = client.getCurrentWorkingDirectory();
			client.performTelnetCommand("cd ..");
			reply = client.getCurrentWorkingDirectory();
			assertTrue("Reply should have subset of previous current working directory", expected.contains(reply));
			
			expected = client.getCurrentWorkingDirectory();
			client.performTelnetCommand("cd \\");
			reply = client.getCurrentWorkingDirectory();
			assertTrue("Reply should have subset of previous current working directory", expected.contains(reply));
		} else {
			// All the tests for Unix platform
			when(mockTC.getOS()).thenReturn("Unix");
			
			client = new TelnetClient(mockTC);
			String reply = client.performTelnetCommand("cd somethingWhichIsNotExpected");
			assertEquals("Error message should be returned", "somethingWhichIsNotExpected - does not exist", reply);
			
			String expected = "";
			reply = client.performTelnetCommand("cd");
			assertEquals("Reply should be empty as we no going to perform any action", expected, reply);
			
			expected = client.getCurrentWorkingDirectory();
			client.performTelnetCommand("cd ..");
			reply = client.getCurrentWorkingDirectory();
			assertTrue("Reply should have subset of previous current working directory", expected.contains(reply));
			
			expected = client.getCurrentWorkingDirectory();
			client.performTelnetCommand("cd /");
			reply = client.getCurrentWorkingDirectory();
			assertTrue("Reply should have subset of previous current working directory", expected.contains(reply));
		}
	}
	
	@Test
	public void testPWDCommand() {
		TelnetClient client = new TelnetClient();
		String expected = client.getCurrentWorkingDirectory();
		String reply = client.performTelnetCommand("pwd");
		assertEquals ("pwd should retrun the current working directory", expected, reply);
	}
	
	@Test
	public void testHelpCommand(){
		TelnetServer mockTCForWindows = mock(TelnetServer.class);
		when(mockTCForWindows.getOS()).thenReturn("Windows");
		
		TelnetClient client = new TelnetClient(mockTCForWindows);
		String reply = client.performTelnetCommand("?");
		assertTrue("Reply should have complete menu, check platform dependent message", reply.contains("dir - List the current working directory."));
		
		TelnetServer mockTCForUnix = mock(TelnetServer.class);
		when(mockTCForUnix.getOS()).thenReturn("SoemthingElse");
		
		TelnetClient client2 = new TelnetClient(mockTCForUnix);
		reply = client2.performTelnetCommand("?");
		assertTrue("Reply should have complete menu, check platform dependent message", reply.contains("ls  - List the current working directory."));
	}
	
	@Test
	public void testHelpMenu(){
		TelnetClient client = new TelnetClient();
		String reply = client.getPrompt();
		assertTrue("Reply should have user prompt", reply.contains(client.getCurrentWorkingDirectory() + "> "));
	}
	
	@Test
	public void testMKDIRCommandWithoutDirName(){
		TelnetClient client = new TelnetClient();
		String reply = client.performTelnetCommand("mkdir");
		assertEquals("We should received empty string", "", reply);
	}
	
	@Test
	public void testMKDIRCommandSuccess(){
		
		TelnetClient client = new TelnetClient() {
			// We really do not want to create a directory so just mock it
			protected String createNewDir(String dirStr) {
				return "";
			}
		};
		String reply = client.performTelnetCommand("mkdir test\\example");
		assertEquals("We should be recieve empty string", "", reply);
	}
	
	@Test
	public void testMKDIRCommandFailed(){
		
		TelnetClient client = new TelnetClient() {
			// We really do not want to create a directory so just mock it
			protected String createNewDir(String dirStr) {
				return "Failure message";
			}
		};
		String reply = client.performTelnetCommand("mkdir test\\example");
		assertEquals("We should be recieve failure ", "Failure message", reply);
	}
	
	@Test
	public void testCommandNotSupported(){
		
		TelnetClient client = new TelnetClient();
		String reply = client.performTelnetCommand("somecrap");
		assertEquals("We should be recieve failure ", "Supplied command is not supported - Enter '?' for list of valid commands", reply);
	}
	
	@Test
	public void testClientCloseFailed() throws IOException {
		TelnetServer mockTS = mock(TelnetServer.class);
		Socket mockSocket = mock(Socket.class);
		doThrow(new IOException()).when(mockSocket).close();
		TelnetClient client = new TelnetClient(mockTS, mockSocket, null);
		
		boolean exceptionThrown = false;
		try {
			client.destroyTelnetClientThread();
		} catch (SocketException e) {
			exceptionThrown = true;
		}
		assertTrue("Exception should be receieved", exceptionThrown);
	}
	
	@Test
	public void testClientCloseSuccess() throws IOException {
		TelnetServer mockTS = mock(TelnetServer.class);
		Socket mockSocket = mock(Socket.class);
		doNothing().when(mockSocket).close();
		TelnetClient client = new TelnetClient(mockTS, mockSocket, null);
		
		boolean exceptionThrown = false;
		try {
			client.destroyTelnetClientThread();
		} catch (SocketException e) {
			exceptionThrown = true;
		}
		assertFalse("Exception should be receieved", exceptionThrown);
	}
	
	@Test
	public void testGetUniqueID() {
		TelnetClient client = new TelnetClient(null, null, "SomeUUID");
		assertEquals("UUID should match", "SomeUUID", client.getMyUniqueId());
	}
	
	@Test
	public void testQUITCommand() throws IOException {
		Socket emptySocket = new Socket();
		TelnetServer mockTS = mock(TelnetServer.class);
		when(mockTS.clientConnected(new TelnetClient())).thenReturn(1);
		when(mockTS.clientDisconnected(new TelnetClient())).thenReturn(0);
		
		TelnetClient client = new TelnetClient(mockTS, emptySocket, "SomeUUID");
		client.start();
		
		String reply = client.performTelnetCommand("quit");
		assertEquals("Connection closed mesage should be recieved", "Connection closed", reply);
	}
	
}
