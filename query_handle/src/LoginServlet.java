
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

import com.fasterxml.jackson.core.JsonProcessingException;

//import it.unibz.inf.stuffie.QueryDB;
 
@WebServlet("/loginServlet")
public class LoginServlet extends HttpServlet {
//	QueryDB obj = new QueryDB();
	
	
    protected void doPost(HttpServletRequest request,
           HttpServletResponse response) throws ServletException, IOException {

        String username = request.getParameter("info");
    	BufferedReader br = new BufferedReader(new InputStreamReader (request.getInputStream()));
    	String str = br.readLine();
    	
        System.out.println("got request"+str);
        if(str==null)
        	return;
        String[] args = str.split(",");
       String result="";
        try {
        	QueryDB.AccessDatabase("bolt://localhost:7687", "neo4j", "singh@123");
        result = QueryDB.getresults(args[0].split(":")[1],args[1].split(":")[1],args[2].split(":")[1],args[3].split(":")[1].replace('}', ' ').trim());
        }
        catch(JsonProcessingException ex) {
        	
        }
//        System.out.println(obj.getresults());
//        System.out.println("username: " + username);
//        System.out.println("start date is: " + startDate);
//        System.out.println("start date is: " + endDate);
// 
        // get response writer
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        System.out.println("resutl is"+result);
        
        
       
        String json_string = "[{\"event\": \"death in J&K\","+
        	  "\"date\": \"2018-09-25\","+
        	  "\"location\": \"Khandwara\","+
        	  "\"link\":\"https://json.org/example\"}]";
        String htmlRespone = "<html>";
        htmlRespone += "<h2>Your username is ghjgjh: " + str + "<br/>";      
        htmlRespone += "start Date is : "  + "</h2>";    
        htmlRespone += "End Date is: "  + "</h2>"; 
        htmlRespone += "</html>";
         
        // return response
        response.getWriter().write(result);
         
    }
    
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    		throws IOException, ServletException{
    	doPost(request, response);
    	
    		}		

 
}