package servers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import data.Info;
import packets.*;

/**
 * In charge of sending out each packet to the user
 */

public class MainServer implements Runnable{

	private static final int PHONE_PORT = 6969;

	private static ConcurrentLinkedQueue<Packet> outgoing = new ConcurrentLinkedQueue<Packet>();
	private HashMap<String,Info> data;

	public MainServer(HashMap<String,Info> data){

		this.data=data;
	}

	public void run() {

		while(true){

			DatagramSocket output=null;

			try {
				output = new DatagramSocket();

				while(true){

					Packet next = outgoing.poll();

					//once we get a packet open a socket to the given ip and port, and send the packet through
					if(next!=null){

						try {

							String targetId=next.getId();
							Info targetInfo=data.get(targetId);

							String targetIp;

							if(targetInfo!=null && (targetIp=targetInfo.getLastKnownIp())!=null){

								//encode the output into a buffer
								byte buf[];
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								ObjectOutputStream oos = new ObjectOutputStream(baos);
								oos.writeObject(next);
								buf=baos.toByteArray();

								//create the UDP packet, send it through the socket and close it
								DatagramPacket dp = new DatagramPacket(buf,buf.length,InetAddress.getByName(targetIp),PHONE_PORT);

								System.out.println("Sending packet to user with id: " + next.getId());
								System.out.println("Last known ip is: "+ targetIp);
								System.out.println("Packet type is:  "+ next.getClass() +"\n");

								output.send(dp);
							}
							else{

								System.out.println("Tried to send packet to user with id: " + next.getId());
								System.out.println("No IP has been entered yet however");
								System.out.println("Packet type is:  "+ next.getClass() + "\n");
							}
						}
						catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			catch (SocketException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			finally{

				if(output!=null){
					output.close();
				}
			}

		}
	}

	public static void addPacket(Packet toSend){

		outgoing.add(toSend);
	}

}
