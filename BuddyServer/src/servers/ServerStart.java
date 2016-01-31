package servers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import data.Info;

public class ServerStart {

	//hash map of id to information on that person
	private static HashMap<String, Info> data = new HashMap<String,Info>(); //the best-est database ever
	private static JSONArray userData;

	public static void main(String args[]){
		
		//writeFile("data.json");
		
		//for each person link their ID to an object containing their info
		data= new HashMap<String,Info>();

		//load the json into data hashmap and return a JSONArray for adding info
		userData=loadFile("data.json");
		userData = userData==null ? new JSONArray() : userData;
		
		startServers();

		System.out.println(data.toString());

	}

	private static void startServers(){

		//starts the server responsible for accepting packets from registered users
		Thread phoneServer = new Thread(new PhoneServer(data,5634,userData));
		phoneServer.start();

		//start the server responsible for sending out requests
		Thread mainServer = new Thread(new MainServer(data));
		mainServer.start();

		//start thread for finding distances
		Thread dist = new Thread(new ClientServer(data));
		dist.start();
	}


	private static JSONArray loadFile(String fileName){

		File inFile = new File(fileName);
		JSONParser reader = new JSONParser();

		try {

			//reads in the file
			FileReader fr = new FileReader(inFile);


			Object obj = reader.parse(fr);
			JSONArray allUsers = (JSONArray) obj;

			//reads users from the array until no more
			for(Object o1:allUsers){

				JSONObject entry =(JSONObject) o1;
				String id = (String) entry.get("id");
				String name = (String) entry.get("name");
				String password = (String) entry.get("password");

				JSONArray friends = (JSONArray) entry.get("friends");

				HashMap<String,String> friendsList = new HashMap<>();

				for(Object o2:friends){

					JSONArray friend = (JSONArray) o2;

					String friendId = (String) friend.get(0);
					String friendName = (String) friend.get(1);
					friendsList.put(friendId, friendName);

				}

				Info user = new Info(null,name,id,friendsList,password);
				data.put(id, user);
			}
			
			return allUsers;


		} catch (FileNotFoundException e) {

			System.out.println("Could not find the data file");
			e.printStackTrace();
		} catch (IOException e) {

			System.out.println("IO Exception");
			e.printStackTrace();
		} catch (ParseException e) {

			System.out.println("File corrupted");
			e.printStackTrace();
		}
		
		return null;

		
	}
	
	private static void writeFile(String name){

		JSONArray allUsers = new JSONArray();
		
		JSONObject user = new JSONObject();
		user.put("id", "0");
		user.put("name", "root");
		user.put("password", "bubba");

		String [][] friends = {{"1","Nick"},{"2","Tal"},{"3","Diane"},{"4","That kid who left"}};
		JSONArray friendsJSON = new JSONArray();

		for(int i=0;i<friends.length;i++){

			JSONArray arr = new JSONArray();
			arr.add(friends[i][0]);
			arr.add(friends[i][1]);

			friendsJSON.add(arr);
		}

		user.put("friends", friendsJSON);

		allUsers.add(user);
		allUsers.add(user);
		
		FileWriter outFile;
		try {

			outFile = new FileWriter(new File("data.json"));
			allUsers.writeJSONString(outFile);
			outFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
