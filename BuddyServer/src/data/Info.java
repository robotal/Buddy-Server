package data;

import java.util.Date;
import java.util.HashMap;

public class Info {

	//coordinates of phone
	private double latitude;
	private double longitude;
	
	//A HashMap of id's you are connected to and distance
	//if distance is -1, the phones are tethered but unaware of locations
	//if distance is -2, there is a request from this phone to the other to tether
	private HashMap<String,Double> myTethers;
	
	//A hashmap of id's to friends
	private HashMap<String,String> friends;
	
	//The last known ip of the user and time
	private String lastKnownIp;
	private long lastTime;
	
	//name and ID of the user
	private String name;
	private String id;
	private String password;
	
	/**
	 * Create a user in the database with an ip and name
	 * This occurs once when the user registers
	 * @param lastKnownIp
	 * @param name
	 * @param id
	 */
	public Info(String lastKnownIp,String name,String id,HashMap<String,String> friends,String password){
		
		latitude = 1000;
		longitude = 1000;
		myTethers = new HashMap<String, Double>();
		this.friends=friends;
		this.lastTime=System.currentTimeMillis();
		this.lastKnownIp=lastKnownIp;
		this.name=name;
		this.id=id;
		this.password=password;
	}
	
	
	public String getPassword(){
		
		return password;
	}
	public long getTime(){
		
		return lastTime;
	}
	
	/**
	 * Return the current latitude (1000 if unknown)
	 */
	public double getLatitude(){
		
		return latitude;
	}
	
	/**
	 * Return the current longitude (1000 if unknown)
	 * @return
	 */
	public double getLongitude(){
		
		return longitude;
	}
	
	/**
	 * 
	 * Return a list of tethers and distane to them
	 * @return
	 */
	public HashMap<String,Double> getTethers(){
		
		return myTethers;
	}
	
	public HashMap<String,String> getFriends(){
		
		return friends;
	}
	
	/**
	 * Return the last ip we connected from
	 * @return
	 */
	public String getLastKnownIp(){
		
		return lastKnownIp;
	}
	
	/**
	 * Return the name of the user
	 * @return
	 */
	public String getName(){
		
		return name;
	}
	
	/**
	 * Return the ID of the user
	 * @return
	 */
	public String getId(){
		
		return id;
	}
	
	/**
	 * Update the current location of the user
	 * @param longitude
	 * @param latitude
	 */
	public void updateLocation(double latitude, double longitude){
		
		if(-90>latitude || 90 < latitude || -180>longitude || 180 < longitude){
			
			throw new IllegalArgumentException("Invalid location");
		}
		
		this.longitude=longitude;
		this.latitude=latitude;
	}
	
	/**
	 * Update the distance between two people if they are tethered between one another
	 * @param id
	 * @param distance
	 */
	public void updateDistance(String id, double distance){
		
		if(!(distance==-1 || distance==-2) && !myTethers.containsKey(id)){
			
			throw new IllegalStateException("Users are not tethered to each other");
		}
		
		myTethers.put(id, distance);
	}
	
	/**
	 * Severs the connection between two users
	 * @param id
	 */
	public void removeTether(String id){
		
		myTethers.remove(id);
	}
	
	/**
	 * Updates the last known ip between two people
	 * @param ip
	 */
	public void updateIp(String ip){
		
		System.out.println("Updating ip address for id: "+ id + " " + ip);
		
		lastKnownIp=ip;
	}
	
	public void updateTime(){
		
		lastTime=System.currentTimeMillis();
	}
	
	public String toString(){
		
		String toRet="";
		toRet+="{"+ "Name: "+name + " id: "+id + " Password:" + password +" coords: "+ latitude+", "+longitude + " Connections: "+myTethers.toString() + " Friends: "+friends.toString()+" IP: "+ lastKnownIp + "}";
		return toRet;
	
	}
}
