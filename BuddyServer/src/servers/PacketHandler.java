package servers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import data.Info;
import packets.*;

/**
 * Responsible for deciding what to do with each packer
 * @author Tal
 *
 */

public class PacketHandler implements Runnable{

	private Packet event;
	private HashMap<String,Info> members;

	public PacketHandler (Packet event, HashMap<String,Info> members){

		this.event=event;
		this.members=members;
	}

	//decides what to do with the packet it receives
	public void run() {

		Info entry = null;
		String ip = event.getIp();
		String id = event.getId();

		synchronized(members){

			entry = members.get(id);

			if(entry!=null){
				
				members.get(id).updateIp(ip);
			}
		}

		switch (event.getPacketType()){

		//update the location of the phone in the database
		//do nothing afterwards, since this is taken care by ClientServer
		case Packet.LOCATION_PACKET:

			LocationPacket phoneLoc = (LocationPacket) event;

			double longitude = phoneLoc.getLongitude();
			double latitude = phoneLoc.getLatitude();
			
			System.out.println("Location packet received from user id: "+event.getId());
			System.out.println("Latitude: "+latitude);
			System.out.println("Longitude: "+longitude);


			synchronized(members){

				entry = members.get(id);
			}

			synchronized(entry){

				entry.updateLocation(latitude, longitude);
				entry.updateTime();
				
				//if the location is an alert, queue up an alert for every tethered person
				if(event instanceof AlertPacket){
					
					HashMap<String,Double> tethers = members.get(id).getTethers();
					for(String friendId:tethers.keySet()){
						
						if(tethers.get(friendId)!=-2){
							
							try {
								AlertPacket alert = new AlertPacket(InetAddress.getLocalHost().getHostAddress(),5634,friendId,"SERVER",latitude,longitude,id);
								MainServer.addPacket(alert);
							} catch (UnknownHostException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
					}
				}

			}

			break;

			//indicate a request from one phone to tether to another
			//put in another pair request packet to the target phone	
		case Packet.PAIR_REQUEST_PACKET:

			PairRequestPacket tetherRequest = (PairRequestPacket) event;

			String tetherId = tetherRequest.getFriendId();

			synchronized(members){

				entry = members.get(event.getId());
			}

			synchronized(entry){

				//check if the devices are already tethered
				if(entry.getTethers().containsKey(tetherId)){

					return;
				}
				//create a pending request (distance -2)
				else{

					entry.updateDistance(tetherId, -2);
				}
			}


			//create a tether request and send it to the other phone
			try {

				PairRequestPacket myRequest = new PairRequestPacket(InetAddress.getLocalHost().getHostAddress(),5634,tetherId,"SERVER",event.getId());
				MainServer.addPacket(myRequest);

			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 


			break;

			//the response from the phone for tethering
			//forward the response to the original phone
		case Packet.PAIR_RESPONSE_PACKET:

			PairResponsePacket response = (PairResponsePacket) event;

			//original person trying to tether
			String requesterId = response.getFriendId();

			//responders id
			String responderId = response.getId();

			//responders answer
			boolean allowTether = response.response();

			//request not allowed, notify requester, remove entry
			if(!allowTether){

				synchronized(members){

					entry = members.get(requesterId);
				}

				//remove the responder id
				synchronized(entry){

					entry.getTethers().remove(responderId);
				}

			}

			//update data entry for both people, changing from -2 to -1
			else{

				Info entry1=null;
				Info entry2=null;

				synchronized(members){

					entry1 = members.get(requesterId);
					entry2 = members.get(responderId);
				}

				synchronized(entry1){

					entry1.updateDistance(responderId, -1.0);
				}

				synchronized(entry2){

					entry2.updateDistance(requesterId, -1.0);
				}

			}

			try {

				//tell the requester what the answer is
				PairResponsePacket toSend = new PairResponsePacket(InetAddress.getLocalHost().getHostAddress(), 5634, requesterId, "SERVER", responderId, allowTether);
				MainServer.addPacket(toSend);

			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			break;

			//create new user if name isn't taken already
		case Packet.LOGIN_PACKET:

			
			Info newUser = new Info(event.getIp(),event.getName(),event.getId(),new HashMap<String,String>());
			
			members.put(event.getId(),newUser);
			
			System.out.println("Created new user with id: " + event.getId());
			System.out.println("User ip is: " + event.getIp());
			System.out.println("Chosen name is: "+ event.getName());

			break;
		}



	}
}
