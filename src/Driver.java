import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.MongoException;

@SuppressWarnings("unused")
public class Driver {

	static Mongo mongo;
	static League league;
	static User user;
	static DBCollection rankings;
	
	public Driver() {
		user = new User(3);
		league = new League(1, 2, 3, 1, 12);
		try {
			mongo = new Mongo("localhost");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		}	
	}
	
	private HashSet<Integer> calculateUserPickNumbers() {
		HashSet<Integer> userPickNumbers = new HashSet<Integer>();
		// for now, only running program until starting roster is full
		for (int i = 1; i <= league.getNumStarters(); i++) {
			int pick;
			if (i % 2 == 1) {
				pick = (league.getNumTeams() * (i - 1)) + user.getDraftPosition();
			}
			else {
				pick = (league.getNumTeams() * i) - user.getDraftPosition() + 1;
			}
			userPickNumbers.add(pick);	
		}
		return userPickNumbers;
	}
	
	private void getPlayerRecommendations(int pickNumber) { 
		// TODO: complete this function
		if (user.getTargetByeWeek() == -1) {
			BasicDBObject query = new BasicDBObject();
			query.put("Drafted", "FALSE");	
		}
	}
	
	private void resetDatabase() {
		DBCursor cursor = rankings.find();
		DBObject update = new BasicDBObject("$set", new BasicDBObject("Drafted", "FALSE"));
		while(cursor.hasNext()) {
			DBObject o = cursor.next();
			DBObject query = new BasicDBObject();
			query.put("First name", o.get("First name"));
			query.put("Last name", o.get("Last name"));
			rankings.update(query, update);
		}
	}
	
	// TODO: Add placeholder value to database that is always listed as Drafted: false
	// in case other users draft someone that's not in the database
	public static void main (String[] args) {
		Driver d = new Driver();
		DB db = mongo.getDB("ffdb");
		rankings = db.getCollection("proj");
		user.setPickNumbers(d.calculateUserPickNumbers());
		d.resetDatabase();

    	BasicDBObject updateToDrafted = new BasicDBObject();
    	updateToDrafted.put("$set", new BasicDBObject("Drafted", "TRUE"));
		int draftPickNumber = 1;
		int roundPickNumber = 1;
		int round = 1;
		boolean isUserTurn;
		DBCursor cur;
		while (user.getRoster().size() < league.getNumStarters()) {
			isUserTurn = false;
			round = (int) Math.ceil((double) draftPickNumber / league.getNumTeams());
			roundPickNumber = draftPickNumber - league.getNumTeams() * (round - 1);
			System.out.println("Round: " + round + " Pick: " + roundPickNumber);
			if (user.getPickNumbers().contains(draftPickNumber)) {
				System.out.println("It's your turn!");
				isUserTurn = true;
			}
			if (isUserTurn) {
				d.getPlayerRecommendations(draftPickNumber);
			}
			System.out.println("Enter player's first and last name: ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String[] input = null;
	        try {
	        	input = br.readLine().trim().split(" ");
	        	if (input.length < 2) {
	        		System.out.println("Incorrect input. Please try again.");
	        		continue;
	        	}
	        	String firstName = input[0];
	        	String lastName = input[1];
	        	BasicDBObject query = new BasicDBObject();
	        	query.put("First name", firstName);
	        	query.put("Last name", lastName);
	        	query.put("Drafted", "FALSE");
	        	cur = rankings.find(query);
	        	if (cur.count() == 0) {
	        		System.out.println("Incorrect spelling or player is already drafted");
	        		continue;
	        	}
	        	// could we get more than 1 result? currently assuming no
	        	else {
	        		draftPickNumber++;
	        		if (isUserTurn) {
		        		DBObject p = cur.toArray().get(0);
		        		Player player = new Player(p);
		        		user.draftPlayer(player);
	        		}
	        		rankings.update(query, updateToDrafted);
	        	}
	        } catch (IOException e) {
	        	System.out.println("Error!");
	        	System.exit(1);
	        }
		}
	}
}
