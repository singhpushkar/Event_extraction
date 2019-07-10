
 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
 
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
@WebServlet("/loginServlet")
public class LoginServlet extends HttpServlet {
 
    protected void doPost(HttpServletRequest request,
           HttpServletResponse response) throws ServletException, IOException {

//        String username = request.getParameter("info");
    	BufferedReader br = new BufferedReader(new InputStreamReader (request.getInputStream()));
    	String str = br.readLine();
       
        System.out.println("got request"+str);
//        System.out.println("username: " + username);
//        System.out.println("start date is: " + startDate);
//        System.out.println("start date is: " + endDate);
// 
        // do some processing here...
         
        // get response writer
        PrintWriter writer = response.getWriter();
         
        // build HTML code
        String htmlRespone = "<html>";
//        htmlRespone += "<h2>Your username is ghjgjh: " + str + "<br/>";      
//        htmlRespone += "start Date is : " + startDate + "</h2>";    
//        htmlRespone += "End Date is: " + endDate + "</h2>"; 
        htmlRespone += "</html>";
         
        // return response
        writer.println(htmlRespone);
         
    }
    
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    		throws IOException, ServletException{
    	doPost(request, response);
    	
    		}		

 
}