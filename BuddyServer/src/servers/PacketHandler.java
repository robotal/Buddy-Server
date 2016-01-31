package servers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.json.simple.*;

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
	private JSONArray users;

	public PacketHandler (Packet event, HashMap<String,Info> members,JSONArray users){

		this.event=event;
		this.members=members;
		this.users=users;
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


							AlertPacket alert = new AlertPacket(PhoneServer.HOST_IP,5634,friendId,"SERVER",latitude,longitude,id);
							MainServer.addPacket(alert);

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
			boolean isTether = tetherRequest.isTether;

			synchronized(members){

				entry = members.get(event.getId());
			}

			synchronized(entry){

				if(isTether){

					//check if the devices are already tethered
					if(entry.getTethers().containsKey(tetherId)){

						return;
					}
					//create a pending request (distance -2)
					else{

						entry.updateDistance(tetherId, -2);
					}
				}else {

					//already friends
					if(entry.getFriends().containsKey(tetherId)){

						return;
					}
					//create pending friend request (missing name)
					else{

						entry.getFriends().put(tetherId, null);
					}

				}
			}


			//create a tether request and send it to the other phone


			PairRequestPacket myRequest = new PairRequestPacket(PhoneServer.HOST_IP,5634,tetherId,entry.getName(),event.getId(),isTether);
			MainServer.addPacket(myRequest);

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
			boolean tether = response.isTether;

			//request not allowed, notify requester, remove entry
			if(!allowTether){

				synchronized(members){

					entry = members.get(requesterId);
				}

				//remove the responder id
				synchronized(entry){


					if(tether){
						entry.getTethers().remove(responderId);
					}
					else{

						entry.getFriends().remove(responderId);
					}
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

					if(tether){
						entry1.updateDistance(responderId, -1.0);
					}
					else{
						entry1.getFriends().put(responderId, entry2.getName());
					}
				}

				synchronized(entry2){

					if(tether){
						entry2.updateDistance(requesterId, -1.0);
					}
					else{
						entry2.getFriends().put(requesterId, entry1.getName());

					}
				}

				synchronized(users){

					for(int i=0;i<users.size();i++){

						JSONObject someUser = (JSONObject) users.get(i);

						if(someUser.get("id").equals(requesterId)){

							JSONArray friendJSON = (JSONArray) someUser.get("friends");
							JSONArray friendEntry = new JSONArray();

							friendEntry.add(responderId);
							friendEntry.add(members.get(responderId).getName());

							friendJSON.add(friendEntry);
							someUser.put("friends", friendEntry);
							users.set(i, someUser);
							break;
						}

					}
					
					//second person
					for(int i=0;i<users.size();i++){

						JSONObject someUser = (JSONObject) users.get(i);

						if(someUser.get("id").equals(responderId)){

							JSONArray friendJSON = (JSONArray) someUser.get("friends");
							JSONArray friendEntry = new JSONArray();

							friendEntry.add(requesterId);
							friendEntry.add(members.get(requesterId).getName());

							friendJSON.add(friendEntry);
							someUser.put("friends", friendEntry);
							users.set(i, someUser);
							break;
						}

					}
					
					FileWriter outFile;
					try {

						outFile = new FileWriter(new File("data.json"));
						users.writeJSONString(outFile);
						outFile.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}

			//tell the requester what the answer is
			PairResponsePacket toSend = new PairResponsePacket(PhoneServer.HOST_IP, 5634, requesterId, members.get(responderId).getName(), responderId, allowTether,tether);
			MainServer.addPacket(toSend);

			break;

			//return a response that contains name and friends list
		case Packet.LOGIN_PACKET:

			LoginPacket loginInfo = (LoginPacket) event;

			String username = loginInfo.getId();
			String password = loginInfo.getPassword();


			String correctPassword = members.get(id).getPassword();

			System.out.println("Login request from id: "+username);
			System.out.println("Attempted password: "+ password + " Correct password: "+ correctPassword);

			synchronized(members){

				//user not registered or incorrect password
				if(!members.containsKey(id) || !(password.equals(correctPassword)) ){

					//login failure
					LoginResponsePacket responsePacket = new LoginResponsePacket(ip,5634,id,null,null,LoginResponsePacket.Result.INCORRECT_INFO);
					MainServer.addPacket(responsePacket);

					System.out.println("Was not able to log in :(\n");
				}
				else{

					LoginResponsePacket responsePacket = new LoginResponsePacket(PhoneServer.HOST_IP,5634,id,members.get(id).getName(),members.get(id).getFriends(),LoginResponsePacket.Result.LOGIN_SUCCESS);
					MainServer.addPacket(responsePacket);
					System.out.println("Succesfully logged in :)\n");

				}
			}

			break;

		case Packet.NEW_USER_PACKET:

			NewUserPacket registration = (NewUserPacket) event;

			//only works if no entry already
			if(!members.containsKey(event.getId())){

				Info newUser = new Info(event.getIp(),event.getName(),event.getId(),new HashMap<String,String>(),registration.getPassword());
				members.put(event.getId(),newUser);

				System.out.println("Created new user with id: " + event.getId());
				System.out.println("User ip is: " + event.getIp());
				System.out.println("Chosen name is: "+ event.getName()+"\n");


				synchronized(users){

					JSONObject newJSON = new JSONObject();

					newJSON.put("name", registration.getName());
					newJSON.put("id", registration.getId());
					newJSON.put("password", registration.getPassword());
					newJSON.put("friends", new JSONArray());
					users.add(newJSON);

					FileWriter outFile;
					try {

						outFile = new FileWriter(new File("data.json"));
						users.writeJSONString(outFile);
						outFile.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else{

				System.out.println("Duplicate attempt to register for id: "+event.getId());
			}

			break;
		}
	}
}
