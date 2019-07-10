package it.unibz.inf.stuffie;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.*;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import org.json.simple.JSONArray; 
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.neo4j.driver.v1.*;
import static org.neo4j.driver.v1.Values.parameters;


// for heideltime
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.unihd.dbs.heideltime.standalone.*;
import de.unihd.dbs.heideltime.standalone.exceptions.*;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;


public class StuffieConsoleRunner {
	
	//for database in neo4j
	static Driver driver;

	private static LinkedHashMap<String, String> commands = new LinkedHashMap<>();
	private static LinkedHashMap<String, String> shorthandCommands = new LinkedHashMap<>();
	private static LinkedHashMap<String, String> loweredKeyCommands = new LinkedHashMap<>();
	private static StringBuilder validModes = new StringBuilder();
	private static StringBuilder validModesAndVals = new StringBuilder();
	private static LinkedHashMap<String, String> validVals = new LinkedHashMap<>();

	private static void initCommands() throws IOException {
		
		try (BufferedReader br = Files.newBufferedReader(Paths.get("resource/console_commands.txt"))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] command = line.split("-");
				commands.put(command[0], command[1]);
				loweredKeyCommands.put(command[0].toLowerCase(), command[0]);
				if (command.length > 2)
					shorthandCommands.put(command[1], command[2]);
			}
		}

		for (Class<?> x : Mode.class.getClasses()) {
			validModesAndVals.append("\t\t" + x.getSimpleName() + "=[");
			validModes.append(x.getSimpleName() + ", ");
			StringBuilder vals = new StringBuilder();
			for (Object enumval : x.getEnumConstants()) {
				vals.append(enumval.toString()).append(", ");
				validModesAndVals.append(enumval.toString()).append("|");
			}
			vals.deleteCharAt(vals.length() - 1).deleteCharAt(vals.length() - 1).append(".");
			validVals.put(x.getSimpleName(), vals.toString());
			validModesAndVals.deleteCharAt(validModesAndVals.length() - 1).append("]\n");
		}
		validModes.deleteCharAt(validModes.length() - 1).deleteCharAt(validModes.length() - 1).append(".");
		validModesAndVals.deleteCharAt(validModesAndVals.length()-1);
	}

	private static Mode[] getCustomModes(String[] args) {
		Mode[] modes = new Mode[args.length];

		int i = 0;
		for (String arg : args) {
			Mode m = getValidMode(arg);
			if (m == null)
				System.exit(1);
			modes[i] = getValidMode(arg);
			i++;
		}

		return modes;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Mode getValidMode(String arg) {
		String[] mode = arg.split("=");

		if (mode.length != 2) {
			System.out.println("Invalid mode change command: " + arg + ".");
		}

		Class<Enum> cls;
		try {
			cls = (Class<Enum>) Class.forName("it.inf.unibz.stuffie.Mode$" + mode[0]);
			System.out.println("Succesfully changed " + mode[0] + " to " + mode[1]);
			return (Mode) Enum.valueOf(cls, mode[1]);
		} catch (ClassNotFoundException e) {
			System.out.println("Mode not found: " + mode[0] + ". The accepted modes are: " + validModes.toString() + "\n");
		} catch (IllegalArgumentException e) {
			System.out.println("Failed to change mode: " + mode[0] + ". Value not found: " + mode[1]
					+ ". The valid values are: " + validVals.get(mode[0]) + "\n");
		}

		return null;
	}

	public static void main(String[] args) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException {

		Properties props = new Properties();
	    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
	    props.setProperty("ner.applyFineGrained", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    
		initCommands();
		Mode[] modes = getCustomModes(args);
		Stuffie stuffie = new Stuffie(modes);

		HeidelTimeStandalone heidelTime = new HeidelTimeStandalone(Language.ENGLISH,DocumentType.COLLOQUIAL,OutputType.TIMEML,"/home/pushkar/heideltime-standalone/config.props",POSTagger.TREETAGGER, true);
		
		
		
		int neo4j_id = 1;
		String news_dump_file = "/home/pushkar/Desktop/stuffie_new/news dump/news_kashmir_21-28May.json";
	
		
		JSONArray all_news = read_json_file(news_dump_file);
		File kashmir_locations_folder = new File("/home/pushkar/Desktop/stuffie_new/villages_processed");
		HashMap<String, ArrayList> locationMap = InitialiseKashmirLocationMap(kashmir_locations_folder);
		
		AccessDatabase("bolt://localhost:7687", "neo4j", "singh@123");
		process_json_array(all_news,locationMap,args,modes,stuffie,heidelTime);

		System.out.print("Bye bye.");
		closeDatabase();
	}
	

public static ArrayList<RelationInstance> run_stuffie(String[] args,String text, Mode[] modes, Stuffie stuffie) throws InstantiationException, IllegalAccessException,
	IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException {


		ArrayList<RelationInstance> output_final = new ArrayList<RelationInstance>();
		
			if(text.isEmpty()) {
				System.out.println("Empty line. Please try again.");
			}
			else if(text.charAt(0) == '<' && text.charAt(text.length() - 1) == '>') {
				text = text.substring(1, text.length() - 1);
				if (text.contains("=")) {
					Mode m = getValidMode(text);
					if (m != null)
						stuffie.setMode(m);
				} else {
					String textLower = text.toLowerCase();
					if (!loweredKeyCommands.containsKey(textLower) && !shorthandCommands.containsKey(textLower)) {
						System.out.println("Invalid command: " + text + ". Enter <h> to list all valid commands.\n");
					} else if (textLower.equals("help") || textLower.equals("h")) {
						for (String command : commands.keySet()) {
							if (shorthandCommands.containsKey(commands.get(command))) {
								System.out.println("\t<" + command + "> <" + commands.get(command) + ">\t"
										+ shorthandCommands.get(commands.get(command)) + "\n");
							} else {
								System.out.println("\t<" + command + ">\t" + commands.get(command) + "\n");
							}
						}
						System.out.println(validModesAndVals.toString() + "\n");
					} else if (textLower.equals("show modes") || textLower.equals("sm")) {
						System.out.println("Current active modes: " + stuffie.currentModesInString() + ".\n");
					}
				}
			} else {
				output_final = stuffie.parseRelation(text);
				//System.out.println(output_final);
			}
			return output_final;
			
	}

    public static ArrayList<String> getTripleComponents(RelationInstance relIns){
		
    	ArrayList<String> tripleComps = new ArrayList<String>();
    	String NULL_CMPNT = "<_>";
    	
    	String id = relIns.getId().toString();
		RelationArgument sub = relIns.getSubject();
		RelationArgument ob = relIns.getObject();
		RelationVerb pred = relIns.getVerb();
		
		tripleComps.add(id);
		
		if(sub!=null) {
			tripleComps.add(sub.toString());
		}
		else {
			tripleComps.add(NULL_CMPNT);
		}
		
		tripleComps.add(pred.toString());
		
		if(ob!=null) {
			tripleComps.add(ob.toString());
		}
		else {
			tripleComps.add(NULL_CMPNT);
		}
    	
    	return tripleComps;
    	
    }

	public static int process_file(int neo4j_id,ArrayList<String> values, String[] args, Mode[] modes, Stuffie stuffie,HeidelTimeStandalone heideltime) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException   {
		
		    	
				for(String value:values) {
					System.out.println(value);
				}
		    	int count = 1;
		    	String[] cell = new String[8];
		    	
		    		cell[0] = values.get(0);
		    		cell[1] = values.get(1);
		    		cell[2] = values.get(2);
		    		cell[3] = values.get(3);
		    		cell[4] = values.get(4);
		    		cell[5] = values.get(5);
		    		cell[6] = values.get(6);
		    		
		    		System.out.println( cell[0] + "\n" + cell[1] + "\n" + cell[2].toString() + "**********");

		            
		            
		    		// Date, Headline, Sentence
		    		String date = cell[0]; 
		            String headline = cell[1];
		            String sentence = cell[2];
		            String news_id = cell[4];
		            String entities = cell[5];
		            String url = cell[6]; 
					
		            
					//Get Locations and remove duplicates
		            //String locations = findLocations(sentence);
		            String locations = cell[3];
		            if(locations!="") {
		            String[] result_array = locations.split(";");
        			List<String> result_list = Arrays.asList(result_array);
        			List<String> newList = result_list.stream().distinct().collect(Collectors.toList());
        			locations = String.join(";", newList);
        			locations = locations.toLowerCase();
		            }
		            
		            
		            
		            
		            /***********Add Event Node Here************** (using neo4j_id) */
		            String neo_id = Integer.toString(neo4j_id);
		            addNode(neo_id,neo_id,"event");
		            addNode(neo_id,date,"date");
		            addNode(neo_id,sentence,"sentence");
		            addNode(neo_id,headline,"headline");
		            addNode(neo_id,locations,"location");
		            addNode(neo_id,news_id,"news_id");
		            addNode(neo_id,entities,"entities");
		            addNode(neo_id,url,"url");
		            
		            
		            
		            
		            /**********STUFFIE starts here****************/
		            
					try {
						
						//get stuffie output
						ArrayList<RelationInstance> triples = run_stuffie(args,sentence.toString(),modes, stuffie);
						
						//valid ids: id's of triples for which subjects and objects are not empty placeholders
						ArrayList<String> validIDs = new ArrayList<String>();
						HashMap<String,String> tripleids_dbids = new HashMap<String,String>(); 
						
						// create unique ID's map
						//initialise local triples id
						int local_triple_id = 1;
						for(RelationInstance relIns: triples) {
							
							ArrayList<String> tripleComps = getTripleComponents(relIns);
							
							String id = tripleComps.get(0).toLowerCase();
							String subject = tripleComps.get(1).toLowerCase();
							String predicate = tripleComps.get(2).toLowerCase();
							String object = tripleComps.get(3).toLowerCase();
							
							//add id to hashmap
							String final_triple_id = neo_id + "_" + Integer.toString(local_triple_id);
							tripleids_dbids.put(id,final_triple_id);
							local_triple_id+=1;
							
							//System.out.println("Here are the facets" + " $$$" + subject + "$$$ " + object + "$$$ " + predicate);
							//System.out.println(relIns.getFacets());
						
						} // for(RelationInstance relIns: triples)
							
						for(RelationInstance relIns: triples) {
							
							ArrayList<String> tripleComps = getTripleComponents(relIns);
							
							String id = tripleComps.get(0).toLowerCase();
							String subject = tripleComps.get(1).toLowerCase();
							String predicate = tripleComps.get(2).toLowerCase();
							String object = tripleComps.get(3).toLowerCase();
							
							/****find id from map and create Triple node
							 event_dbid = triple db_id (local_triple_id)******/
							String event_dbid = tripleids_dbids.get(id);
							String event_name = relIns.toString().toLowerCase();
							addTripleNode(event_dbid, "triple", neo_id);
							
							
							//check for <ctx#> and #link in subject and object
							
							Pattern p = Pattern.compile("\\#(.*)");
							Matcher p_s = p.matcher(subject);
							Matcher  p_o = p.matcher(object);
							
					    	Pattern ctx = Pattern.compile("(\\<(ctx#.*)\\>)");
							Matcher ctx_s = ctx.matcher(subject);
							Matcher ctx_o = ctx.matcher(object);
							
							// #id and <ctx#id> dont occur together in the same part of triple
							
							
							/***PROCESS SUBJECT
							//check for <ctx#id> in subject, else check for #id***/
							if(ctx_s.find()) {
								//System.out.println(ctx_s.group() + "***********");
								String sb = subject.replace(ctx_s.group(), "");
								sb = sb.replace("<", "");
								sb = sb.replace(">", ">");
								String sb_lemma = "";
								sb = sb.replace(";", "");
								sb = sb.trim();
								if(!sb.contentEquals("<_>")) {
									Lemmatizer doc = new Lemmatizer();
							    	sb_lemma = doc.lemmatize(subject);
								}
								//System.out.println(ctx_s.group() + "***********sub" + sb);
								addSVO(event_dbid,sb,"subject",sb_lemma);
							}
							else{
								if(p_s.find()) {
									
									/**create node e' and linked: e=>e' with link as 'has subject'**/
									
									//System.out.println(p_s.group());
									
									String temp_link_id = p_s.group();
									String link_id = process_entity(temp_link_id);
									
									//System.out.println(link_id);
									
									//map for link_id's node id
									String db_linkID = tripleids_dbids.get(link_id);
									addSpRelation(event_dbid,db_linkID,"hasSubject");
									
								}
								else {
									/***create subject node***/
									String sb_lemma = "";
									subject = subject.replace(";", "");
									subject = subject.trim();
									if(!subject.contentEquals("<_>")) {
										Lemmatizer doc = new Lemmatizer();
								    	sb_lemma = doc.lemmatize(subject);
									}
									addSVO(event_dbid,subject,"subject",sb_lemma);
								}
							}
							
							//PROCESS PREDICATE
							predicate = predicate.replace(";", "");
							predicate = predicate.trim();
							String pr_lemma = "";
							if(!predicate.contentEquals("<_>")) {
								Lemmatizer doc = new Lemmatizer();
						    	pr_lemma = doc.lemmatize(predicate);
							}
							addSVO(event_dbid,predicate,"predicate",pr_lemma);
							
							//PROCESS OBJECT
							if(ctx_o.find()) {
									
									String ob = object.replace(ctx_o.group(), "");
									ob = ob.replace("<", "");
									ob = ob.replace(">", ">");
									String ob_lemma = "";
									ob = ob.replace(";", "");
									ob = ob.trim();
									if(!ob.contentEquals("<_>")) {
										Lemmatizer doc = new Lemmatizer();
										ob_lemma = doc.lemmatize(ob);
									}
									System.out.println(ctx_o.group() + "***********,obb" + ob);
									addSVO(event_dbid,ob,"object",ob_lemma);
								
							}
							else {
								if(p_o.find()) {
									
									/**create node e' and linked: e=>e' with link as 'has object'**/
									
									//System.out.println(p_o.group());
									
									String temp_link_id = p_o.group();
									String link_id = process_entity(temp_link_id);
									
									//System.out.println(link_id);
									
									String linkID = tripleids_dbids.get(link_id);
									addSpRelation(event_dbid,linkID,"hasObject");

								}
								else {
									/***create Object node**/
									object = object.replace(";", "");
									object = object.trim();
									String ob_lemma = "";
									if(!object.contentEquals("<_>")) {
										Lemmatizer doc = new Lemmatizer();
								    	pr_lemma = doc.lemmatize(object);
									}
									addSVO(event_dbid,object,"object",ob_lemma);
								}
								
								
								
							}
							
							//PROCESS FACETS
							System.out.println(relIns.getFacets());
							for(RelationArgument value: relIns.getFacets()) {
								
								System.out.println(value);
								if(value!=null) {
								String[] data = (value.toString()).split(";");
								
								String connector = data[0].toLowerCase();
								connector = connector.replace(";", "");
								connector = connector.trim();
								
								String target = data[1].toLowerCase();
								target = target.replace(";", "");
								target = target.trim();
								
								process_facet(connector,target,event_dbid,tripleids_dbids);
								}
							}
							
							
													
						}
						
					// Stuffie output try-catch
					}catch(Exception e) {
					   e.printStackTrace();
					}
			   neo4j_id = neo4j_id+1;
		       System.out.println(neo4j_id);
				
		
			return neo4j_id;
	}
	
	
	public static void process_facet(String connector, String target, String event_dbid, HashMap<String,String> tripleids_dbids) {
		
		Pattern mtch_links = Pattern.compile("\\#(.*)");
		Matcher  t_ml = mtch_links.matcher(target);
		
		Pattern mtch_ctx = Pattern.compile("(\\<(ctx#.*)\\>)");
		Matcher  t_ctx = mtch_ctx.matcher(target);
		
		if(t_ctx.find()) {
			//if target has a context, create context link as well as target node
			
			
			String targ = target.replace(t_ctx.group(), "");
			targ = targ.replace("<", "");
			targ = targ.replace(">", ">");
			String targ_lemma = "";
			targ = targ.replace(";", "");
			targ = targ.trim();
			if(!targ.contentEquals("<_>")) {
				Lemmatizer doc = new Lemmatizer();
				targ_lemma = doc.lemmatize(targ);
			}
			addFacet(event_dbid,connector,target,"facet",targ_lemma);
			System.out.println(t_ctx.group());
		}
		else {
			if(t_ml.find()) {
				
				// if target has link, create that event node and link with connector
		
				
				String temp_link_id = t_ml.group();
				String link_id = process_entity(temp_link_id);
				
				
				//System.out.println(link_id);
				
				String linkID = tripleids_dbids.get(link_id);
				addSpFacet(event_dbid,connector,linkID,"facet");

			}
			else {
				String locations =  findLocations(target);
				String target_lemma="";
				//create Facet node
				if(locations=="") {
					if(!target.contentEquals("<_>")) {
						Lemmatizer doc = new Lemmatizer();
						target_lemma = doc.lemmatize(target);
					}
				addFacet(event_dbid,connector,target,"facet",target_lemma);
				}
			}
		}
		
	
		
	}
	
    public static String process_entity(String entity) {
    	
    	entity = entity.replace("#","");
    	entity = entity.replace(";","");
    	entity = entity.replace(">","");
    	entity = entity.trim();
    	return entity;
    }
    
	
	public static String processDates(String refdate,String text,HeidelTimeStandalone heidelTime) {
		String date = "";
		
		
		
		//String text = "I tried out many options yesterday, but failed. I can't plan this day after "
				//+ "tomorrow, so I have to do this today or tomorrow ";
		try {
		Date refDate= new SimpleDateFormat("dd-MM-yyyy").parse(refdate);
		String xmlDoc = heidelTime.process(text, refDate);
		
		System.out.println(xmlDoc);
		ArrayList<String> listDates = getDates(xmlDoc,refdate);
		//System.out.println(listDates);
		
		
		if(listDates.size()==1) {
			return listDates.get(0).toString();
		}
		else {
			return refdate;
		}
		}catch(Exception e) {
			e.printStackTrace();
			return refdate;
		}
		
	}
	
	public static ArrayList<String> getDates(String xmlDoc,String refdate){
		
		//process xmlDoc to get rid of meta tags
		String[] list = xmlDoc.split("\n");
		StringBuilder processedDoc = new StringBuilder();
		for(int i=2;i<list.length;i++) {
			processedDoc.append(list[i]);
		}
			
		System.out.println(processedDoc.toString());
		
		/* list of dates to be returned  */
		ArrayList<String> dates = new ArrayList<String>();
	    try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(processedDoc.toString())));
					
			NodeList nList = doc.getElementsByTagName("TimeML");
	
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);					
				System.out.println("\nCurrent Element :" + nNode.getNodeName());
				
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					NodeList x3List = eElement.getElementsByTagName("TIMEX3");
					
					for (int i = 0; i < x3List.getLength(); i++) {
						Node x3Node = x3List.item(i);								
						System.out.println("\nCurrent Element :" + x3Node.getNodeName());
							
						if (x3Node.getNodeType() == Node.ELEMENT_NODE) {
							Element x3Element = (Element) x3Node;							
							System.out.println("Name : " + x3Element.getTextContent());
							System.out.println("Date : " + x3Element.getAttribute("value"));
							
							/* convert date in string format to Date object
							 */
							 String str_date = x3Element.getAttribute("value");
							 SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-YYYY");
							 
							 String[] date_check = str_date.split("-");
							 if(date_check.length>2) {
								 Date date=new SimpleDateFormat("yyyy-MM-dd").parse(str_date);
								 String strDate= formatter.format(date);
								 dates.add(strDate);
							 }
							 else {
								 str_date = str_date.replace("W", "");
								 Date date=new SimpleDateFormat("yyyy-ww").parse(str_date);
								 String strDate= formatter.format(date);
								 dates.add(strDate);
							 }
						}
					}
				}
			}
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	dates.add(refdate);
	    	return dates;		
	    }
	    return dates;
	  }
	
	 public static void AccessDatabase(String uri, String user, String password){
	        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
	 }
	 
	 /**** Adding AVO nodes and Linking SVO nodes to Triple Node ***/
	 public static void addSVO(String id, String name, String type,String lemma) {
		 try (Session session = driver.session())
	        {
	            // Wrapping Cypher in an explicit transaction provides atomicity
	            // and makes handling errors much easier.
	            try (Transaction tx = session.beginTransaction())
	            {
	                if(type.contentEquals("subject")) {
	                	tx.run("MERGE (n:EntityNode {name: {x}})" + "SET n.lemma = {lemma_form}" + "RETURN n", parameters("x", name,"lemma_form",lemma));
	                	tx.run("MATCH (a:Triple {triple_id: {myid} }),(b:EntityNode {name: {name}})" + "MERGE (a)-[r:HAS_SUBJECT]->(b)" + "RETURN a,b",parameters("myid",id,"name",name));
	                }
	                else if(type.contentEquals("object")) {
	                	tx.run("MERGE (n:EntityNode {name: {x}})" + "SET n.lemma = {lemma_form}" + "RETURN n", parameters("x", name,"lemma_form",lemma));
	                	tx.run("MATCH (a:Triple {triple_id: {myid} }),(b:EntityNode {name: {name}})" + "MERGE (a)-[r:HAS_OBJECT]->(b)" + "RETURN a,b",parameters("myid",id,"name",name));
	                }
	                else if(type.contentEquals("predicate")) {
	                	tx.run("MERGE (n:Predicate {name: {x}})" + "SET n.lemma = {lemma_form}" + "RETURN n", parameters("x", name,"lemma_form",lemma));
	                	tx.run("MATCH (a:Triple {triple_id: {myid} }),(b:Predicate {name: {name}})" + "MERGE (a)-[r:HAS_PREDICATE]->(b)" + "RETURN a,b",parameters("myid",id,"name",name));
	                }
	            	
	                tx.success();  // Mark this write as successful.
	            }
	            session.close();
	        }
	 }
	 
	 
	 // Creating only Event Node
	 private static void addNode(String id, String name, String type){
	        // Sessions are lightweight and disposable connection wrappers.
	        try (Session session = driver.session())
	        {
	            // Wrapping Cypher in an explicit transaction provides atomicity
	            // and makes handling errors much easier.
	            try (Transaction tx = session.beginTransaction())
	            {

	                if(type.contentEquals("event")) {
	                	tx.run("MERGE (n:Event {dbid: {x}})", parameters("x", id));
	                }
	                
	                else if(type.contentEquals("date")) {
	                	String date = name;
	                	tx.run("MERGE (n:Event {dbid: {x}})" + "SET n.date = {y}" + "RETURN n",parameters("x",id,"y",date));
	                	//tx.run("MERGE (n:Event {dbid: {x}})" + "SET n.date = {y}" + "RETURN n",parameters("x",id,"y",date));
	                }
	                
	                else if(type.contentEquals("sentence")) {
	                	String sentence = name;
	                	tx.run("MERGE (n:Event {dbid: {x}})" + "SET n.sentence = {y}" + "RETURN n",parameters("x",id,"y",sentence));
	                }
	                
	                else if(type.contentEquals("headline")) {
	                	String headline = name;
	                	tx.run("MERGE (n:Event {dbid: {x}})" + "SET n.headline = {y}" + "RETURN n",parameters("x",id,"y",headline));
	                }
	                
	                else if(type.contentEquals("location")) {
	                	String location = name;
	                	tx.run("MERGE (n:Event {dbid: {x}})" + "SET n.location = {y}" + "RETURN n",parameters("x",id,"y",location));
	                }
	            	
	                else if(type.contentEquals("news_id")) {
	                	String news_id = name;
	                	tx.run("MERGE (n:Event {dbid: {x}})" + "SET n.news_id= {y}" + "RETURN n",parameters("x",id,"y",news_id));
	                }
	                
	                else if(type.contentEquals("entities")) {
	                	String entities = name;
	                	tx.run("MERGE (n:Event {dbid: {x}})" + "SET n.entities = {y}" + "RETURN n",parameters("x",id,"y",entities));
	                }
	                
	                else if(type.contentEquals("url")) {
	                	String url = name;
	                	tx.run("MERGE (n:Event {dbid: {x}})" + "SET n.url = {y}" + "RETURN n",parameters("x",id,"y",url));
	                }
	                
	            	//tx.run("MERGE (b:Person {name: 'mynewnode'})");
	                tx.success();  // Mark this write as successful.
	            }
	            session.close();
	        }
	 }
	 
	 //Creating Triple Node
	 private static void addTripleNode(String id, String type, String dbid) {
		 
		 try (Session session = driver.session())
	        {
	            // Wrapping Cypher in an explicit transaction provides atomicity
	            // and makes handling errors much easier.
	            try (Transaction tx = session.beginTransaction())
	            {
	            	
	            	if(type.contentEquals("triple")) {
	                	tx.run("MERGE (n:Triple {triple_id: {x}})", parameters("x", id));
	                	tx.run("MATCH (a:Event {dbid: {myid} }),(b:Triple {triple_id: {local_triple_id}})" + "MERGE (a)-[r:HAS_TRIPLE]->(b)" + "RETURN a,b",parameters("myid",dbid,"local_triple_id",id));
		                
	                }
	            
	            tx.success();  // Mark this write as successful.
	            }
            session.close();
        }
		 
	 }
	 
	 
	  
	    
	 
	    public static void addFacet(String id, String connector, String target, String type, String lemma) {
	    	try (Session session = driver.session())
	        {
	    		try (Transaction tx = session.beginTransaction())
	            {
	    			if(type.contentEquals("facet")) {
	                	tx.run("MERGE (n:Facet {name: {x}})" + "SET n.lemma = {lemma_form}" + "RETURN n", parameters("x", target,"lemma_form",lemma));
	                	tx.run("MATCH (a:Triple  {triple_id: {myid} }),(b:Facet {name: {facet_name}})" + "MERGE (a)-[r:CONNECTING_CLAUSE {name:{rel_name}}]->(b)" + "RETURN a,b",parameters("myid",id,"facet_name",target,"rel_name",connector));
	                }
	    			tx.success(); 
	            }
	    		session.close();
	        }
	    }
	 
	    public static void addSpFacet(String nodeid1, String connector, String nodeid2, String type) {
	    	try (Session session = driver.session())
	        {
	    		try (Transaction tx = session.beginTransaction())
	            {
	    			if(type.contentEquals("facet")) {
	    				tx.run("MERGE (n:Triple  {dbid: {x}})", parameters("x", nodeid2));
	    				tx.run("MATCH (a:Triple  {dbid: {myid} }),(b:Triple  {triple_id: {myid2}})" + "MERGE (a)-[r:CONNECTING_CLAUSE {name:{rel_name}}]->(b)" + "RETURN a,b",parameters("myid",nodeid1,"myid2",nodeid2,"rel_name",connector));	
	    			}
	    			tx.success(); 
	            }
	    		session.close();
	        }
	    }
	    
	 	public static void addSpRelation(String nodeId1, String nodeId2, String relation) {
	 		try (Session session = driver.session()){
	 			
	 			try (Transaction tx = session.beginTransaction())
	            {
	 				 tx.run("MERGE (n:Triple  {triple_id: {x}})", parameters("x", nodeId2));
	 				 if(relation.contentEquals("hasSubject")) {
		                	tx.run("MATCH (a:Triple  {triple_id: {myid} }),(b:Triple  {triple_id: {myid2}})" + "MERGE (a)-[r:HAS_SUBJECT]->(b)" + "RETURN a,b",parameters("myid",nodeId1,"myid2",nodeId2));
		             }
	 				if(relation.contentEquals("hasObject")) {
	                	tx.run("MATCH (a:Triple  {triple_id: {myid} }),(b:Triple  {triple_id: {myid2}})" + "MERGE (a)-[r:HAS_OBJECT]->(b)" + "RETURN a,b",parameters("myid",nodeId1,"myid2",nodeId2));
	 				}
	            }
	 		    
	 			session.close();
	        }
	 	}
	 	
	 	/**********************************************/
	 	/**********************************************/
	 	/**********************************************/
	 	/**************Preprocessing + Locations Function + pass to main**************/
	 	/**********************************************/
	 	/**********************************************/
	 	/**********************************************/
	 	/**********************************************/
	 	
	 	
	 	
	 	 public static List<String> GetSentences(String input){
	         List<String> sentenceList = new ArrayList<>();
	         // creates a StanfordCoreNLP object
	         Properties props = new Properties();
	         props.put("annotators", "tokenize, ssplit");
	         StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	         // create an empty Annotation just with the given text
	         Annotation document = new Annotation(input);
	         pipeline.annotate(document);
	         List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
	         for(CoreMap sentence:sentences){
	             List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
	             String originalString = SentenceUtils.listToOriginalTextString(labels);
	             sentenceList.add(originalString);
	         }
	         System.out.println(sentenceList);
	         return sentenceList;
	     }
	 	 
	 	public static int pass_to_process(int neo4j_id,ArrayList<String> pass_values,String[] args, Mode[] modes, Stuffie stuffie,HeidelTimeStandalone heideltime) {
	 		//System.out.println(pass_values.get(0)+ " ^^^^^^^^^^ " + pass_values.get(1) + " ^^^^^^^^^^ " + pass_values.get(2) +  " ^^^^^^^^^^ " + pass_values.get(3));
	 		try {
	 			neo4j_id = process_file(neo4j_id,pass_values,args,modes,stuffie,heideltime);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	 		return neo4j_id;
	 	}
	 	
	 	public static void  process_json_array(JSONArray all_news,HashMap<String, ArrayList> locationMap, String[] args, Mode[] modes, Stuffie stuffie,HeidelTimeStandalone heideltime) {
			//int count = 0;
	 		int neo4j_id = 1;
			for (Object o : all_news)
			  {
				HashMap<String, ArrayList<ArrayList<String>>> map = new HashMap<String, ArrayList<ArrayList<String>>>(); 
				//if(count<5) {
			    JSONObject news_item = (JSONObject) o;

			   	Object title_news =  news_item.get("title");
			   	String title = "";
			   	if(title_news!=null) {
			   		title = title_news.toString();
			   	}
			   	System.out.println(title);
			   	//System.out.println("HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH");
			   	//System.out.println("HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH");
			   	//System.out.println("HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH");
			   	
			   	Object news_id_orig = news_item.get("_id");
			   	String news_id = "";
			   	if(news_id_orig!=null) {
			   		news_id = news_id_orig.toString();
			   		news_id = news_id.replace("{\"$oid\":\"","");
			   		news_id = news_id.replace("\"}", "");
			   		System.out.println(news_id);
			   	}
			   	
			   	
			   	Object entities_list = news_item.get("dbpedia");
			   	String entities = "";
			   	if(entities!=null) {
			   		System.out.println("yess");
			   		entities = entities_list.toString();
			   		entities = entities.replaceAll("\"", "");
			   		entities = entities.replace("]", "");
			   		entities = entities.replace("[", "");
			   	}
			   	
			   	//System.out.println(entities+" " +news_id);
			   	
			   	Object url_ob =  news_item.get("url");
			   	String url = "";
			   	if(url_ob!=null) {
			   		url = url_ob.toString();
			   	}
			   	
			   	Object date_news =  news_item.get("publishAt");
			   	String date = "";
			   	if(date_news!=null) {
			   		date = date_news.toString();
			   		date = date.split("T")[0];
			   		date = date.replace("{\"$date\":\"", "");
			   	}
			   	
			   	Object content_news =  news_item.get("whole_content");
			   	String content = "";
			   	if(content_news!=null) {
			   		content = content_news.toString();
			   	}
			   	
			   	if(content!="") {
			   		List<String> sentences = GetSentences(content);
			   		
			   		for(String sentence: sentences) {
			   			
			   			String locations = "";
			   			locations = findLocations_kashmir(sentence,locationMap);
			            
			   			ArrayList<String> instance = new ArrayList<String>();
			            instance.add(date);
			            instance.add(sentence);
			            instance.add(locations);
			            instance.add(news_id);
			            instance.add(entities);
			            instance.add(url);
			            System.out.println(date+ " " +locations);
			            
			            if(locations.contentEquals("") || locations==null) {
			            	//System.out.println("yes");
			            	instance.add("0");
			            }
			            else {
			            	instance.add("1");
			            }
			            
			            if(map.containsKey(title)) {
			            	ArrayList<ArrayList<String>> this_list = map.get(title);
			            	this_list.add(instance);
			            	map.put(title, this_list);
			            }
			            else {
			            	ArrayList<ArrayList<String>> new_list = new ArrayList<ArrayList<String>>();
			            	new_list.add(instance);
			            	map.put(title,new_list);
			            }
			            
			   		}
			   		
			   	}
			   	
			   	// 1 iteration ends here
				neo4j_id = iterateOver(neo4j_id,map,"abc",args,modes,stuffie,heideltime);
				
			  }
			//return map;
		}
	 	
		public static JSONArray  read_json_file(String filename) {
			JSONParser parser = new JSONParser();
			//JsonParser to convert JSON string into Json Object
			JSONArray array_file = null;
			try {
				array_file = (JSONArray) parser.parse(new FileReader(filename));

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}catch (org.json.simple.parser.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return array_file;
		}
		
	 	
	 	public static int iterateOver(int neo4j_id,HashMap<String, ArrayList<ArrayList<String>>> map,String writefile,String[] args, Mode[] modes, Stuffie stuffie,HeidelTimeStandalone heideltime) {
			
			// headline is key, [date,sentence,location,1/0]
			
			Iterator it = map.entrySet().iterator();
		    while (it.hasNext()) {
		    	
		    	
		    	Map.Entry pair = (Map.Entry)it.next();
		    	String headline = pair.getKey().toString();
		    	
		    	ArrayList<ArrayList<String>> array_values = (ArrayList<ArrayList<String>>) pair.getValue();
		    	
		    	int count_index = 0;
		    	
		    	for(ArrayList<String> array: array_values) {
		    		//System.out.println(array.get(3));
		    		//if location does not exist
		    		if(array.get(3)!="1") {
		    			//System.out.println("Not found");
		    			int f_low = count_index + 1;
		    			int f_high = array_values.size();
		        		String f_result = checkForward(array_values,f_low,f_high);
		        		
		        		int b_low = 0;
		        		int b_high = count_index - 1;
		        		String b_result = checkBackward(array_values,b_low,b_high);
		        		
		        		
		        		if(f_result!="" && b_result!="") {
		        			String result = f_result + ";" + b_result;
		        			
		        			String[] result_array = result.split(";");
		        			List<String> result_list = Arrays.asList(result_array);
		        			List<String> newList = result_list.stream().distinct().collect(Collectors.toList());
		        			String loc = String.join(";", newList);
		        			
		        			array.set(2,loc);
		        		}
		        		else if(f_result=="" && b_result!="") {
		        			//System.out.println(b_result+ " "+ array.get(0));
		        			array.set(2, b_result);
		        		}
		        		else if(f_result!="" && b_result=="") {
		        			//System.out.println(f_result+ " "+ array.get(0));
		        			array.set(2, f_result);
		        		}
		        		else if(f_result=="" && b_result=="") {
		        			
		        		}
		        	
		        	//write_to_csv(array.get(0),headline,array.get(1),array.get(2),writefile);
		        		//pass to main 
		            	ArrayList<String> pass_values = new ArrayList<String>();
		            	pass_values.add(array.get(0));
		            	pass_values.add(headline);
		            	pass_values.add(array.get(1));
		            	pass_values.add(array.get(2));
		            	pass_values.add(array.get(3));
		            	pass_values.add(array.get(4));
		            	pass_values.add(array.get(5));
		            	neo4j_id = pass_to_process(neo4j_id,pass_values,args,modes,stuffie,heideltime);
		            	
		    		}
		    		else {
		    			//pass to main 
		            	ArrayList<String> pass_values = new ArrayList<String>();
		            	pass_values.add(array.get(0));
		            	pass_values.add(headline);
		            	pass_values.add(array.get(1));
		            	pass_values.add(array.get(2));
		            	pass_values.add(array.get(3));
		            	pass_values.add(array.get(4));
		            	pass_values.add(array.get(5));
		            	neo4j_id = pass_to_process(neo4j_id,pass_values,args,modes,stuffie,heideltime);
		    			//write_to_csv(array.get(0),headline,array.get(1),array.get(2),writefile);
		    		}
		        	count_index+=1;
		        } 
		    
		    	it.remove();
		    }
		    return neo4j_id;
		}
	 	
	 	public static String checkForward(ArrayList<ArrayList<String>> array, int low, int high) {
			String result = "";
			
			if(low<=high) {
				for (int i = low; i<high; i++ ) {
					if(array.get(i).get(3)=="1") {
						return array.get(i).get(2);
					}
				}
			}
			
			return result;
		}
		
		public static String checkBackward(ArrayList<ArrayList<String>> array, int low, int high) {
			String result = "";
			
			if(low<=high) {
				for (int i = high; i>=low; i-- ) {
					if(array.get(i).get(3)=="1") {
						return array.get(i).get(2);
					}
				    //System.out.println(array.get(i));
				}
			}
			
			return result; 
		}
		
		public static HashMap<String, ArrayList> InitialiseKashmirLocationMap(File folder){
			
			HashMap<String, ArrayList> locationMap = new HashMap<String, ArrayList>();
			String state = "kashmir";
			//read files from processed files folder
			File[] fileNames = folder.listFiles();
		    for(File file : fileNames){
		    		
		    		// read content from file into arraylist
		    		String district = file.getName().replace(".txt","");
		    		List<String> villages = new ArrayList<String>();
		    		ArrayList<String> finalVillages = new ArrayList<String>();
		            try {
		            	try(BufferedReader br  = new BufferedReader(new FileReader(file))){
		                    String strLine;
		                    while((strLine = br.readLine()) != null){
		                     villages.add(strLine);
		                    }
		              }
		            	List<List<String>> distrcits_villages = Arrays.asList(villages);
		            	List<String> updated_districts = convertToLowerCase(distrcits_villages.get(0));
		            	finalVillages.addAll(updated_districts);
		        		
		        		locationMap.put(district, finalVillages);
		            } catch (IOException e) {
		                // TODO Auto-generated catch block
		                e.printStackTrace();
		            }
		    }
			
			
			return locationMap;
			
		}

		public static String findLocations_kashmir(String sentence,HashMap<String, ArrayList> locationMap) {
			
			String state = "kashmir";
			
			ArrayList<String> detectedLocations = new ArrayList<String>();
			String finalLocations = "";
			for (Map.Entry<String,ArrayList> entry : locationMap.entrySet()) { 
		        //System.out.println("Key = " + entry.getKey() + 
		                         //", Value = " + entry.getValue()); 
				ArrayList<String> templocs = entry.getValue();
				String district = entry.getKey();
				//System.out.println(district);
				
				ArrayList<String> subdistricts = new ArrayList<String>();

				ArrayList<String> detectedLocations_v2 = new ArrayList<String>();
				
				if(sentence.toLowerCase().contains(state.toLowerCase())) {
					detectedLocations.add(state.toLowerCase());
					//System.out.println("Yes1");
				}
				for(String loc: templocs) {
					String[] temp_loc_split = loc.toLowerCase().split(",");
					String[] sentence_words = sentence.toLowerCase().split(" ");
					ArrayList<String> sentenceWords = new ArrayList<String>(Arrays.asList(sentence_words));
					if(!temp_loc_split[0].toLowerCase().contentEquals(temp_loc_split[1].toLowerCase())) {
						if(sentenceWords.contains(temp_loc_split[0].toLowerCase())) {
						
						
							if(district.toLowerCase().contentEquals(temp_loc_split[1].toLowerCase())) {
								//System.out.println("loop1");
								loc = temp_loc_split[0] + "," + district.toLowerCase() + "," + state.toLowerCase();
								detectedLocations.add(loc);
							}
							else {
								loc = loc + "," + district.toLowerCase() + "," + state.toLowerCase();
								detectedLocations.add(loc);
						}
						//System.out.println("loc++++" + loc);		
						
						}
						if(sentenceWords.contains(temp_loc_split[1].toLowerCase()) && !subdistricts.contains(temp_loc_split[1])) {
							//System.out.println("Yessss");
							subdistricts.add(temp_loc_split[1]);
							if(district.toLowerCase().contentEquals(temp_loc_split[1].toLowerCase())) {
								//System.out.println("loop2");
								detectedLocations_v2.add(district.toLowerCase() + "," + state.toLowerCase());
							}
							else {
								detectedLocations_v2.add(temp_loc_split[1] + "," + district.toLowerCase() + "," + state.toLowerCase());
							}
						}
					}
				
				if(detectedLocations.size()>0) {
					List<String> locationsDistinct = detectedLocations.stream().distinct().collect(Collectors.toList());
					if(locationsDistinct.size()>0) {
						finalLocations = String.join(";" , locationsDistinct);
					}
				}
				else {
					if(detectedLocations_v2.size()>0) {
						List<String> locationsDistinct = detectedLocations_v2.stream().distinct().collect(Collectors.toList());
						if(locationsDistinct.size()>0) {
							finalLocations = String.join(";" , locationsDistinct);
						}				
					}
				}
				}	
			}
			System.out.println("************************************"+ finalLocations);
			return finalLocations;
			
		}


		public static List<String> convertToLowerCase(List<String> districtList){
			
			ListIterator<String> iterator = districtList.listIterator();
		    while (iterator.hasNext())
		    {
		        iterator.set(iterator.next().toLowerCase());
		    }
			return districtList;
			
			
		}
	 	
	 
		public static void printMap(Map mp) {
		    Iterator it = mp.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        System.out.println(pair.getKey());
		        //System.out.println(pair.getKey() + " = " + pair.getValue());
		        ArrayList<ArrayList<String>> value = (ArrayList<ArrayList<String>>) pair.getValue();
		        for(ArrayList<String> array: value) {
		        	for(String item: array) {
		        		System.out.println(item + "*****");
		        	}
		        	System.out.println("\n");
		        }
		        System.out.println("Next Item");
		        it.remove(); // avoids a ConcurrentModificationException
		    }
		}
		

		
		public static void write_to_csv(String date, String headline, String sentence, String location, String filename) {
			//String filename = "C:\\Users\\Reen\\Desktop\\DBFiles\\DBNew\\Locations\\SentencesFebruary2016V2.csv";
			File file = new File(filename); 
			try { 
		        FileWriter outputfile = new FileWriter(file,true); 
		  
		        CSVWriter writer = new CSVWriter(outputfile); 
		  
		        String[] data = {date,headline,sentence,location};
		        writer.writeNext(data);		  
		        writer.close();
		        outputfile.close();
		    } 
		    catch (IOException e) { 
		        // TODO Auto-generated catch block 
		        e.printStackTrace(); 
		    } 
			
		}
		
		public static String findLocations(String sentence) {
			
			String output = "";
			Properties props = new Properties();
		    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
		    props.setProperty("ner.applyFineGrained", "false");
		    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		    CoreDocument doc = new CoreDocument(sentence);
		    pipeline.annotate(doc);
		    //System.out.println("---");
		    //System.out.println("entities found");
		    try {
		    for (CoreEntityMention em : doc.entityMentions()) {
		    	if(em.entityType().contentEquals("LOCATION")) {
		    		if(output.contentEquals("")) {
		    			output = em.text().toString();
		    		}
		    		else {
			    		output = output + ";" + em.text().toString();
		    		}
		    		//System.out.println("\tdetected entity: is a location"+em.text()+"\t"+em.entityType());
		    	}
		    }
		    }catch(Exception e){
		    	e.printStackTrace();
		    }
		    //System.out.println(output);
		    return output;
			
		}
		
	    public static void closeDatabase(){
	        // Closing a driver immediately shuts down all open connections.
	        driver.close();
	    }
	    
	   
	
}





	
	
