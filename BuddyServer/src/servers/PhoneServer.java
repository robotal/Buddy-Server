
package servers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import data.Info;
import packets.*;


/**
 * Responsible for receiving packets from each phone
 * @author Tal
 *
 */

public class PhoneServer implements Runnable{

	private int MAX_UDP_DATAGRAM_LEN=4096;
	private int UDP_SERVER_PORT;
	
	private JSONArray users;
	
	public static String HOST_IP;
	
	private HashMap<String,Info> data;

	public PhoneServer(HashMap<String,Info> data,int port,JSONArray users){

		this.data=data;
		this.UDP_SERVER_PORT=port;
		this.users=users;
		
		try {
			HOST_IP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}


	public void run(){

		runUdpServer();
	}

	private void runUdpServer() {

		try {
			System.out.println("Starting phone server on ip: " + InetAddress.getLocalHost().getHostAddress() + ", Port is:"+UDP_SERVER_PORT);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}




		DatagramSocket socket = null;

		//in case the socket goes down, make another one
		while(true){

			try {

				//start the server on the given port 
				socket = new DatagramSocket(UDP_SERVER_PORT);

				byte[] incomingPacket = new byte[MAX_UDP_DATAGRAM_LEN];

				//used to read in objects
				DatagramPacket packet = new DatagramPacket(incomingPacket, incomingPacket.length);

				//accept packets forever while server is running
				while(true){

					Packet incoming = receivePacket (socket,packet,incomingPacket);


					new Thread(new PacketHandler(incoming,data,users)).start();

				}

			} catch (SocketException e) {

				System.out.println("Socket closed unexpectedly");
				e.printStackTrace();
			}

		}
	}

	private Packet receivePacket(DatagramSocket socket,DatagramPacket packet,byte[] incomingPacket) {

		try {

			socket.receive(packet);



			//read the object sent over
			ObjectInputStream ois;

			ois = new ObjectInputStream(new ByteArrayInputStream(incomingPacket));


			Object o1 = ois.readObject();


			Packet clientInfo = (Packet) o1;

			System.out.println("New connection from ip: " + clientInfo.getIp());
			int clientPort = clientInfo.getPort();
			System.out.println("Port found: " + clientPort);

			System.out.println("Packet received of type: " +clientInfo.getClass());
			System.out.println("Client id: " + clientInfo.getId());
			System.out.println("Client name: " +clientInfo.getName() + "\n");

			return clientInfo;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
}