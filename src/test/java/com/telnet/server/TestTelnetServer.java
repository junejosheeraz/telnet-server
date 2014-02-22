package com.telnet.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class TestTelnetServer {
	
	@Test
	public void testListConnections() {
        TelnetServer server = new TelnetServer(12345);
        server.start();
        
        Socket echoSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
		try {
            echoSocket = new Socket("127.0.0.1", 12345);
            out = new PrintWriter(echoSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
            		                    echoSocket.getInputStream()));
            out.println("pwd");
            in.readLine();
		} catch (UnknownHostException e) {
            //ignore
        } catch (IOException e) {
        	//ignore
        }
        
		String reply = server.listAllConnections();
		assertTrue("Number of connections - 1", reply.contains("Active Connections = 1"));
		assertEquals("Number of connections - 1", 1, server.getNumberOfConnections());
		
		//Cleanup
		try {
			out.close();
			in.close();
			echoSocket.close();
		} catch (IOException e) {
			//ignore
		}
        
		reply = server.shutDown();
		assertEquals("Server shutdown message should be recieved", "Goodbye", reply);
    }
	
	@Test
	public void testServerShutdownFailed() throws IOException {
		ServerSocket mockSC = mock(ServerSocket.class);
		doThrow(new IOException()).when(mockSC).close();
		
		TelnetServer server = new TelnetServer(mockSC, 12345);
		String reply = server.shutDown();
		assertEquals("Error message should be recieved", "Failed", reply);
	}
	
	@Test
	public void testServerAcceptFailedBecauseMaxConnReached() throws IOException {
		TelnetServer server = new TelnetServer(12345){
			public int getNumberOfConnections() {
				return 100;
			}
		};
        server.start();
        
        String reply = "";
        Socket echoSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
		try {
            echoSocket = new Socket("127.0.0.1", 12345);
            out = new PrintWriter(echoSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
            		                    echoSocket.getInputStream()));
            reply = in.readLine();
		} catch (UnknownHostException e) {
            //ignore
        } catch (IOException e) {
        	//ignore
        }
        
		//Cleanup
		try {
			out.close();
			in.close();
			echoSocket.close();
		} catch (IOException e) {
			//ignore
		}
        
		assertEquals("Max connection reached message should be recieved", "Too many users", reply);
		
		reply = server.shutDown();
		assertEquals("Server shutdown message should be recieved", "Goodbye", reply);
	}
	
	@Test
	public void testAddNewActiveConnection() {
        
		TelnetClient mockClient = mock(TelnetClient.class);
		when(mockClient.getMyUniqueId()).thenReturn("SomeUUID");
		
		TelnetServer server = new TelnetServer();
        server.clientConnected(mockClient);
        
        String reply = server.listAllConnections();
        
        assertTrue("Client should be present", reply.contains("SomeUUID"));
        assertTrue("Number of active should be one(1)", reply.contains("Active Connections = 1"));
    }
	
	@Test
	public void testDisconnectActiveConnectionWithNoId() {
        TelnetServer server = new TelnetServer();
		String reply = server.disconnectClient(null);
        
        assertEquals("Error message should be receieved", "Client id is mandatory input", reply);
    }
	
	@Test
	public void testDisconnectActiveConnectionWithInvalidId() {
        
		TelnetClient mockClient = mock(TelnetClient.class);
		when(mockClient.getMyUniqueId()).thenReturn("SomeUUID");
		
		TelnetServer server = new TelnetServer();
		server.clientConnected(mockClient);
		
		String reply = server.disconnectClient("Does not matter");
        
        assertEquals("Error message should be receieved", "Invalid client id provided", reply);
    }
	
	@Test
	public void testDisconnectActiveConnectionFailure() throws SocketException {
        
		TelnetClient mockClient = mock(TelnetClient.class);
		when(mockClient.getMyUniqueId()).thenReturn("SomeUUID");
		doThrow(new SocketException()).when(mockClient).destroyTelnetClientThread();
		
		TelnetServer server = new TelnetServer();
		server.clientConnected(mockClient);
		
		String reply = server.disconnectClient("SomeUUID");
        
        assertEquals("Failure message should be receieved", "Failed", reply);
    }
	
	@Test
	public void testDisconnectActiveConnectionSuccess() throws SocketException {
        
		final TelnetServer server = new TelnetServer();
		
		final TelnetClient mockClient = mock(TelnetClient.class);
		when(mockClient.getMyUniqueId()).thenReturn("SomeUUID");
		doAnswer(new Answer(){
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				server.clientDisconnected(mockClient);
				return null;
			}
			
		}).when(mockClient).destroyTelnetClientThread();
				
		server.clientConnected(mockClient);
		
		String reply = server.disconnectClient("SomeUUID");
        
        assertEquals("Success message should be receieved", "Success", reply);
        assertEquals("Number of active connections", 0, server.getNumberOfConnections());
    }
	
	@Test
	public void testDisconnectAllActiveConnectionFailed() throws SocketException {
        
		TelnetClient mockClient = mock(TelnetClient.class);
		when(mockClient.getMyUniqueId()).thenAnswer(new Answer<String>() {
			 	public String answer(InvocationOnMock invocation) {
			 		return (UUID.randomUUID().toString());
			    }
		});
		doThrow(new SocketException()).when(mockClient).destroyTelnetClientThread();
		
		TelnetServer server = new TelnetServer();
		server.clientConnected(mockClient);
		
		// Make sure we have active connection in list
		String reply = server.listAllConnections();
		assertTrue("Number of active client connection - 1", reply.contains("Active Connections = 1"));
		assertEquals("Number of active client connection - 2",1, server.getNumberOfConnections());

		reply = server.disconnectAll();
        
        assertEquals("Failure message should be receieved", "Failed", reply);
        assertEquals("Number of active client connection - 3", 1, server.getNumberOfConnections());
    }
	
	@Test
	public void testDisconnectAllActiveConnectionSuccess() throws SocketException {
        
		final TelnetServer server = new TelnetServer();
		
		// Mock the TelnetClient 1
		final TelnetClient mockClient = mock(TelnetClient.class);
		when(mockClient.getMyUniqueId()).thenAnswer(new Answer<String>() {
			 	public String answer(InvocationOnMock invocation) {
			 		return (UUID.randomUUID().toString());
			    }
		});
		doAnswer(new Answer(){
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				server.clientDisconnected(mockClient);
				return null;
			}
			
		}).when(mockClient).destroyTelnetClientThread();
		
		// Mock the TelnetClient 2
		final TelnetClient mockClient2 = mock(TelnetClient.class);
		when(mockClient2.getMyUniqueId()).thenAnswer(new Answer<String>() {
			 	public String answer(InvocationOnMock invocation) {
			 		return (UUID.randomUUID().toString());
			    }
		});
		doAnswer(new Answer(){
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				server.clientDisconnected(mockClient2);
				return null;
			}
			
		}).when(mockClient2).destroyTelnetClientThread();
		
		
		server.clientConnected(mockClient);
		server.clientConnected(mockClient2);
		
		// Make sure we have active connection in list
		String reply = server.listAllConnections();
		assertTrue("Number of active client connection - 1", reply.contains("Active Connections = 2"));
		assertEquals("Number of active client connection - 2", 2, server.getNumberOfConnections());

		reply = server.disconnectAll();
        
        assertEquals("Failure message should be receieved", "Success", reply);
        assertEquals("Number of active client connection - 3", 0, server.getNumberOfConnections());
    }
	
	@Test
	public void testGetOS() {
		TelnetServer server = new TelnetServer();
		String reply = server.getOS();
		assertNotNull("OS should have some value", reply);
	}
	
}
