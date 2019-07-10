package it.unibz.inf.stuffie;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.neo4j.driver.v1.Values.parameters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import com.opencsv.CSVWriter;

public class QueryDB {

	static Driver driver;
	
	public String getresults()
	{
		String query_final = "OPTIONAL MATCH (Node) where (Node.lemma contains 'death' OR Node.lemma contains 'strike') " + "with Node " + "MATCH (Event)--(Node) where Event.location contains 'Handwara' " + "return Event.headline, Event.sentence, Event.location, Event.date, Node.name";
		return "Query call ho gaya";
	}
	
	
	 public static void main(String args[]) throws JsonProcessingException {
		
			AccessDatabase("bolt://localhost:7687", "neo4j", "123456");
			
			String query1 = "MATCH (Node) where Node.lemma contains 'death' " + "OPTIONAL MATCH (Event)--(Node) " + "OPTIONAL MATCH (Event)-[r:HAS_SUBJECT]-(Subject) " + "OPTIONAL MATCH (Event)-[r1:HAS_OBJECT]-(Object) " 
					+ "OPTIONAL MATCH (Event)-[r2:HAS_PREDICATE]-(Predicate)"
					+ "OPTIONAL MATCH (Event)-[r3:CONNECTING_CLAUSE]-(Facet)" +  "return Event.headline,Event.sentence,Event.location,Event.date,Node.name,Node.dbid,Subject.name,Predicate.name,Object.name,Facet.name";
			String query2 = "MATCH (n) where n.lemma contains 'protest' " + "OPTIONAL MATCH (x)--(n) " + "return x.location,x.date,x.sentence,n.name,x.dbid";
			String query3 = "MATCH (n) where n.lemma contains 'strike' " + "OPTIONAL MATCH (x)--(n) " + "return x.location,x.date,x.sentence,n.name,x.dbid";
			String query4 = "MATCH (n) where n.lemma contains 'meet' " + "OPTIONAL MATCH (x)--(n) " + "return x.location,x.date,x.sentence,n.name,x.dbid";
			String query5 = "MATCH (n) where n.lemma contains 'bomb' " + "OPTIONAL MATCH (x)--(n) " + "return x.location,x.date,x.sentence,n.name,x.dbid";
			String query6 = "MATCH (n) where n.lemma contains 'stone pelt' " + "OPTIONAL MATCH (x)--(n) " + "return x.location,x.date,x.sentence,n.name,x.dbid";
			String query7 = "MATCH (n) where n.lemma contains 'pellet' " + "OPTIONAL MATCH (x)--(n) " + "return x.location,x.date,x.sentence,n.name,x.dbid";
			
			
			String query_final = "OPTIONAL MATCH (Node) where (Node.lemma contains 'death' OR Node.lemma contains 'strike') " + "with Node " + "MATCH (Event)--(Node) where Event.location contains 'Handwara' " + "return Event.headline, Event.sentence, Event.location, Event.date, Node.name";
			//String query8 = "MATCH (n) where date(n.date) = date('25-03-2016')" + "return n.sentence,n.date,n.location";
			String query8 = "MATCH (n) where n.location contains 'Kashmir' " + "return n.sentence,n.date,n.location,n.dbid,n.headline";
			
			
			ArrayList<Map<String, Object>> dataMap = tryQuery(query1);
			ObjectMapper objectMapper = new ObjectMapper();
			String json = objectMapper.writeValueAsString(dataMap);
			System.out.println(json);
			
			closeDatabase();
	 }
	
	 public static void AccessDatabase(String uri, String user, String password){
	        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
	 }
	 
	 public static ArrayList<Map<String, Object>> tryQuery(String query) {
		 String query_result = "";
		 Map<String,Object> map = null;
		 ArrayList<Map<String, Object>> dataMap = new ArrayList<Map<String, Object>>();
		 try (Session session = driver.session())
	        {
	             
	    			StatementResult result = session.run(query);
	    	        while (result.hasNext()){
	    	        	Record record = result.next();
	    	        	map = record.asMap();
	    	        	/*for (Map.Entry<String,Object> entry : map.entrySet())  
	    	                System.out.println("Key: " + entry.getKey() + 
	    	                                 ", Value: " + entry.getValue());*/ 
	    	        	dataMap.add(map);
	    	        	//System.out.println("\n\n");
	    	        	//write_to_file(map);
	    	        	//query_result = record.toString();
		    	        //System.out.println(query_result);
	    	        }
	                    	    	        
	    		session.close();
	        }
		 return dataMap;
	 }
	 
	 public static void write_to_file(Map<String,Object> map) {
			
		
		 	String file = "C:\\Users\\Reen\\Desktop\\output1.txt";
	        // If the file doesn't exists, create and write to it
			// If the file exists, truncate (remove all content) and write to it
	        try (FileWriter writer = new FileWriter(file,true);
	             BufferedWriter bw = new BufferedWriter(writer)) {
	        	for (Map.Entry<String,Object> entry : map.entrySet())  
	                bw.write("Key: " + entry.getKey() + 
	                                 ", Value: " + entry.getValue() + "\n"); 
	        		bw.write("**\n");
	            bw.write("\n\n");
	            bw.close();
	        } catch (IOException e) {
	            System.err.format("IOException: %s%n", e);
	        }

	        
		 
	 }
	 
    public static void closeDatabase(){
        // Closing a driver immediately shuts down all open connections.
        driver.close();
    }
	
}
