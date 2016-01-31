package servers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import data.Info;
import packets.FriendUpdatePacket;
import packets.FriendUpdatePacket.FriendLocation;

/**
 * Runs forever, calculating new tether distances and sending FriendUpdatePackets to the main server
 * @author Tal
 *
 */

public class ClientServer implements Runnable {

	private HashMap<String,Info> data;

	public ClientServer(HashMap<String,Info> data){

		this.data=data;
	}


	public void run() {


		//run forever, calculating distances and populating location updates for each user
		while(true){

			try {

				//wait 5 seconds before each iteration
				Thread.sleep(5000);

				System.out.println("WOKE UP FROM SLUMBER\n");

				synchronized(data){

					System.out.println(data.size() + " users to check for.");

					//loop through each user, calculate distance between tethered users
					for(String userId:data.keySet()){

						System.out.println("USER ID : "+userId);

						Info entry = data.get(userId);

						double lat1 = entry.getLatitude();
						double lon1 = entry.getLongitude();

						HashMap<String,Double> connections =entry.getTethers();

						System.out.println(connections.size() + " people tethered to user "+ userId);

						if(!connections.isEmpty()){

							ArrayList<FriendLocation> locs = new ArrayList<FriendLocation>();

							//loop through all neighbors
							for(String neighborId:connections.keySet()){

								Info entry2 = data.get(neighborId);
								double distance = connections.get(neighborId);

								//not a pending connection request
								if(distance!=-2){

									double lat2=entry2.getLatitude();
									double lon2=entry2.getLongitude();


									double newDist = distance(lat1,lon1,lat2,lon2);
									
									System.out.println("Distance from id: "+userId + " to user id is " + newDist + "miles");
									
									connections.put(neighborId, newDist);

									FriendLocation neighborLoc = new FriendLocation(entry2.getId() ,lat2,lon2,newDist,entry2.getTime()); 
									locs.add(neighborLoc);
								}
							}


							//create a friendUpdatePacket to send 

							FriendLocation[] friendArr = locs.toArray(new FriendLocation[0]);

							System.out.println(friendArr.length + " location in array.\n");

							if(friendArr.length>0){
								FriendUpdatePacket locationsUpdate = new FriendUpdatePacket(InetAddress.getLocalHost().getHostAddress(), 5634, userId, "SERVER",friendArr);
								MainServer.addPacket(locationsUpdate);
							}
						}
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static double distance(double lat1, double lon1, double lat2, double lon2) {

		if(lat1==1000 || lon1==1000 || lat2 == 1000 || lon2==1000){

			return -1;
		}

		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;

		return (dist);
	}

	private static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	private static double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}


}
