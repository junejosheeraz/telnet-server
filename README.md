										README
										======
					
								Telnet Server 0.0.1-SNAPSHOT
								----------------------------
					
Welcome to the Telnet Server 0.0.1-SNAPSHOT! The aim is to provide support 
for simultanous access to the telnet server where each user can access the 
hosting server with support of few basic commands like ls, dir, pwd, cd, mkdir.
This is a multi-threaded system which will entertain max of five(5) concurrent 
connections. 


Coding Restrictions
-------------------
	1. 	Application should be portable to Unix/Linux and Windows platform
	
	2. 	The use of the classes that invoke native commands (e.g. Runtime or 
		ProcessBuilder) is not allowed
		

JDK Version notes
-----------------

The Telnet Server support JDK 1.6 or higher and tested with the same version.


Requirements
------------

You can use Maven 2.2.1 to build the project. Note that project build is tested
with Maven 2.2.1 and Maven 3.0.4.


Build
------

		UNIX/Linux
		----------
		
		1. 	Unzip the telnet-server.zip archive
		
		2. 	Set the JAVA_HOME to the JDK 1.6:
				
				export JAVA_HOME=<JDK_Install_Dir>
		
		3. 	Set MAVEN_HOME to maven install dir:
				
				export MAVEN_HOME=<Maven_Install_Dir>
				
		3. 	Add JAVA_HOME/bin and MAVEN_HOME/bin into PATH:
				
				export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
				
		4. 	Navigate to the extacted 'telnet-server' directory you got after Step 1 and
			apply following command to build the source, run unit test and release 
			the project artifact:
				
				mvn clean install

		Windows
		-------
		
		1. 	Unzip the telnet-server.zip archive
		
		2. 	Set the JAVA_HOME to the JDK 1.6:
				
				export JAVA_HOME=<JDK_Install_Dir>
		
		3. 	Set MAVEN_HOME to maven install dir:
				
				export MAVEN_HOME=<Maven_Install_Dir>
				
		3. 	Add JAVA_HOME/bin and MAVEN_HOME/bin into PATH:
					export PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%
				
		4. 	Navigate to the extacted 'telnet-server' directory you got after Step 1 and
			apply following command to build the source, run unit test and release 
			the project artifact:

				mvn clean install
	
	Build Extras
	------------
	
	During the 'package' phase, build will run 'Cobertura' coverage reports plugin
	with following checks:
			<check>
				<branchRate>75</branchRate>
				<lineRate>85</lineRate>
				<haltOnFailure>true</haltOnFailure>
				<totalBranchRate>75</totalBranchRate>
				<totalLineRate>85</totalLineRate>
				<packageLineRate>85</packageLineRate>
				<packageBranchRate>75</packageBranchRate>
			</check>
	
	If any one of the above condition is not fulfilled, build will fail and force the
	developer to write more efficient unit tests. The report can be found under 
	'telnet-server/target/site' diectory and can be viewed within a web browser.
	
			
Execution
---------			
		
	Server Side
	-----------
	
		1.  Successfull build will produce executable telnet-server jar under
			'target\telnet-server-0.0.1-SNAPSHOT.jar', run the jar as follows to start 
			the telnet-server:
				java -jar target\telnet-server-0.0.1-SNAPSHOT.jar <Server_Port>
			
			Note: If <Server_Port> is not provided server will be started on default
			port = 4444
		
		2.  Above command will present server side menu as follows:
				Telnet server started successfully on port 4455

				1. List all active clients
				2. Disconnect a single client
				3. Disconnect all clients
				4. Shutdown server and quit
				>
			
			Note: From here you can control the server.
		
		3. 	Now you can connect using any Telnet client like Putty, Secure CRT etc
			to use the telnet-server application.

	Client Side
	-----------
	
		1.	Once connected, client will be presented with the following menu:
				
				Welcome to the Test Telnet Server, following commands are available...

				?   - Display this help menu.
				dir - List the current working directory.
				cd <DIRECTORY_NAME> - Change the current working directory to the provided arguments.
				pwd - Display the current working directory.
				mkdir <DIRECTORY_NAME> - Create a directory.
				quit - To disconnect.


				C:\Temp\Test\telnet-server> 

		2. Client can now use the server.

		
Problems?
---------

If running the server on Windows platform, note that the ls/dir command output will be scatered 
across the screen.

Enjoy!