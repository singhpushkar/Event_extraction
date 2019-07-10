
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONArray; 
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class TestJson {
	
public static void main(String args[]) {
	String news_dump_file = "/home/pushkar/Desktop/stuffie_new/news dump/json_sample.json";
	
	JSONArray all_news = read_json_file(news_dump_file);
}
	
	public static JSONArray  read_json_file(String filename) {
		JSONParser parser = new JSONParser();
		//JsonParser to convert JSON string into Json Object
		JSONArray array_file = null;
		try {
			array_file = (JSONArray) parser.parse(new FileReader(filename));
			System.out.println("chal gaya:");

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

}
