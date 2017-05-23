import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Node implements Runnable {
	private static final String WAIT_RESPONSE = "wait";
	private static final String AFFIRMATIVE_RESPONSE = "printed";
	private static final String NEGATIVE_RESPONSE = "negative";

	private static final String AFFIRMATIVE_MESSAGE = AFFIRMATIVE_RESPONSE + "\n";
	private static final String WAIT_MESSAGE = WAIT_RESPONSE + "\n";
	private static final String NEGATIVE_MESSAGE = NEGATIVE_RESPONSE + "\n";

	private static final long WAIT_TIME = 500;

	private ServerSocket serverSocket;
	private boolean isPrinted = false;
	private final String ipAddressesFile;
	private final int portNumber;
	private long creationTime;

	// Constructor for a Node: requires ipAddressesFile and portNumber.
	// Also creationTime is kept for deciding on its age.
	public Node(String ipAddressesFile, int portNumber) {
		this.ipAddressesFile = ipAddressesFile;
		this.portNumber = portNumber;
		creationTime = System.currentTimeMillis();
	}


	// Node class implements Runnable.

	// <<<<< run#1 >>>>>>>>>
	@Override
	public void run() {
		try {
			// run method here first forces a thread to sleep here for a random time, within 1000 ms of interval.
			Thread.sleep(Math.abs(new Random().nextLong() % 1000));
		} catch (InterruptedException e1) {
		}

		new Thread(new Runnable() { //this thread initializes the socket communication; by calling initServerSocket method.

			@Override
			public void run() {
				try {
					initServerSocket();
				} catch (IOException e) {
				}
			}
		}).start();

		// within the run#1 method, a new Thread is initiated and started.
		// This runnable requires to fetch other nodes and check if it is printed. By default, isPrinted is false.
		new Thread(new Runnable() {
			List<NodeInfo> readOtherNodesInfo = readOtherNodesInfo(ipAddressesFile);

			public void run() {
				isPrinted = checkIfPrinted(readOtherNodesInfo);
				if (isPrinted) {
					System.out.println("other machine printed " + portNumber + " " + creationTime);
				} else {
					isPrinted = true;
					System.out.println("we are started " + portNumber + " " + creationTime);
				}

			}
		}).start();

	} //	<<<<<<<<< end of run#1 >>>>>>>>>>>>>>>>>

	// Here, server behaviour is given. it only asks for a portNumber for the serverSocket creation
	// Since rest of the socket connections are also sharing the same local.
	// They only differentiate from each other by their portNumbers.

	public void initServerSocket() throws IOException {
		// A new serverSocket object is created with portNumber given.
		serverSocket = new ServerSocket(portNumber);
		while (true) { // connectionSocket is accepting serverSocket instances with its accepting method. It requires the portNumber, only.
			Socket connectionSocket = serverSocket.accept();

			// with this connectionSocket, a new thread starts running; it uses BufferedReader and DataOutputStream.
			// it then accepts connections with respect to their portNumbers.
			new Thread(new Runnable() {

				@Override
				public void run() {

					// inFromClient and outToClient are are data streams.
					//
					try {
						BufferedReader inFromClient = new BufferedReader(
								new InputStreamReader(connectionSocket.getInputStream()));
						DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
						while (true) {
							// the InputStreamReader called inFromClient is being read and all line is kept in a string called message.
							// creationTime is also read.
							String message = inFromClient.readLine();
							long creation = Long.parseLong(message);
							// output stream outToClient is used here.
							outToClient = new DataOutputStream(connectionSocket.getOutputStream());
							if (isPrinted) // if the isPrinted flag is true, then print an affirmative message.
								outToClient.writeBytes(AFFIRMATIVE_MESSAGE);
							else if (creation < creationTime) //if the incoming node's creationTime is younger, print a wait message.
								outToClient.writeBytes(WAIT_MESSAGE);
							else // if the isPrinted flag is not true and its creationTime is younger, its claim is not accepted.
								outToClient.writeBytes(NEGATIVE_MESSAGE);
						}
					} catch (NumberFormatException e) {
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			}).start();

		}
	}

	// client behaviour is implementec within this method.
	// with the client behaviour, a node list also given as NodeInfo args list.
	//
	private boolean checkIfPrinted(List<NodeInfo> readOtherNodesInfo) {
		for (NodeInfo nodeInfo : readOtherNodesInfo) {
			try {
				Socket clientSocket = new Socket(nodeInfo.ipAddress, nodeInfo.portNumber);
				DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				whileloop: {
					while (true) {
						outToServer.writeBytes(Long.toString(creationTime) + "\n");
						String response = inFromServer.readLine();
						switch (response) {
						case WAIT_RESPONSE:
							Thread.sleep(WAIT_TIME);
							break;
						case AFFIRMATIVE_RESPONSE:
							clientSocket.close();
							System.out.println("aff massage from " + nodeInfo.portNumber);
							return true;
						case NEGATIVE_RESPONSE:
							clientSocket.close();
							break whileloop;

						default:
							assert(false);
						}
					}
				}
			} catch (Exception e) {
				continue;
			}
		}
		return false;

	}

	//
	private List<NodeInfo> readOtherNodesInfo(String filePath) {
		List<NodeInfo> otherNodes = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
			while (br.ready()) {
				String readLine = br.readLine();
				String[] split = readLine.split(" ");
				otherNodes.add(new NodeInfo(split[0], split[1]));
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return otherNodes;
	}

	// NodeInfo is an inner class to define a data structure for a socket (Ip address & port number)
	// NodeInfo is used within several methods, and given as a List of type NodeInfo to those methods.
	class NodeInfo {
		String ipAddress;
		int portNumber;

		public NodeInfo(String ipAddress, String portNumber) {
			this.ipAddress = ipAddress;
			this.portNumber = Integer.parseInt(portNumber);
		}
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage of this program is: {path to ip addresses file} {portnumber to run}");
			System.exit(0);
		}

		//ipAddressesFile file's path is fetched as the first argument, of type: String.,
		//and portNumber is passed as an integer.
		String ipAddressesFile = args[0];
		int portNumber = Integer.parseInt(args[1]);

		System.out.print("Node started: ");
		System.out.print(ipAddressesFile + " ");
		System.out.println(portNumber);

		//When the valid arguments are given to the main function, a new node is instantiated with the run method.
		new Node(ipAddressesFile, portNumber).run();

	}

}
