/**
 * 
 */
package org.bgu.ise.ddb.registration;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.bgu.ise.ddb.ParentController;
import org.bgu.ise.ddb.User;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

/**
 * @author Alex
 *
 */
@RestController
@RequestMapping(value = "/registration")
public class RegistarationController extends ParentController{
	
	private static long DAY_IN_MS = 1000 * 60 * 60 * 24;
	
	/**
	 * Aux method for code reuse
	 * @return 
	 */
	private MongoCollection<Document> getUserCollection(){
		return ParentController.getDB().getCollection("Users");
	}


	/**
	 * The function checks if the username exist,
	 * in case of positive answer HttpStatus in HttpServletResponse should be set to HttpStatus.CONFLICT,
	 * else insert the user to the system  and set to HttpStatus in HttpServletResponse HttpStatus.OK
	 * @param username
	 * @param password
	 * @param firstName
	 * @param lastName
	 * @param response
	 */
	@RequestMapping(value = "register_new_customer", method={RequestMethod.POST})
	public void registerNewUser(
			@RequestParam("username")    String username,
			@RequestParam("password")    String password,
			@RequestParam("firstName")   String firstName,
			@RequestParam("lastName")    String lastName,
			                             HttpServletResponse response){

		System.out.println(username+" "+password+" "+lastName+" "+firstName);
		
		try {
			
			boolean userExists = isExistUser(username);
			
			if (userExists) {
				
				HttpStatus status = HttpStatus.CONFLICT;
				response.setStatus(status.value());
				
			} else {
				
				MongoCollection<Document> collection = getUserCollection();

				Document user = new Document("username", username);
				user.append("password", password);
				user.append("first_name", firstName);
				user.append("last_name", lastName);
				user.append("registration_date", new Date());

				collection.insertOne(user);
				
				HttpStatus status = HttpStatus.OK;
				response.setStatus(status.value());
				
			}
			
		} catch (IOException e) {
			
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			response.setStatus(status.value());
			
		}
	}

	/**
	 * The function returns true if the received username exist in the system otherwise false
	 * @param username
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "is_exist_user", method={RequestMethod.GET})
	public boolean isExistUser(
			@RequestParam("username") String username
			) throws IOException {
		
		System.out.println("isExistUser(\"" + username +"\")");
		
		boolean result = false;
		
		MongoCollection<Document> collection = getUserCollection();
		Document d = collection.find(new Document("username", username)).first();
		result = (d != null);

		return result;

	}

	/**
	 * The function returns true if the received username and password match a system storage entry, otherwise false
	 * @param username
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "validate_user", method={RequestMethod.POST})
	public boolean validateUser(
			@RequestParam("username")    String username,
			@RequestParam("password")    String password
			) throws IOException {
		System.out.println(username+" "+password);
		
		boolean result = false;

		MongoCollection<Document> collection = getUserCollection();
		Document d = collection.find(new Document("username", username).append("password", password)).first();
		result = (d != null);

		return result;

	}

	/**
	 * The function retrieves number of the registered users in the past n days
	 * @param days
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "get_number_of_registred_users", method={RequestMethod.GET})
	public int getNumberOfRegistredUsers(@RequestParam("days") int days) throws IOException{
		System.out.println(days+"");
		int result = 0;
		
		MongoCollection<Document> collection = getUserCollection();
		FindIterable<Document> d = collection.find(Filters.gt("registration_date", new Date(new Date().getTime() - (days * DAY_IN_MS))));
		MongoCursor<Document> it = d.iterator();
		
		Document user = it.tryNext();
		while (user != null) {
			result ++;
			user = it.tryNext();
		}

		return result;

	}

	/**
	 * The function retrieves all the users
	 * @return
	 */
	@RequestMapping(value = "get_all_users",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(User.class)
	public User[] getAllUsers(){
		List<User> result = new ArrayList<User>();

		getUserCollection().find().forEach(
				(Document doc) -> {
					result.add(new User(doc.getString("username"), doc.getString("first_name"), doc.getString("last_name")));
				});
		
		User[] users = new User[result.size()];
		result.toArray(users);
		
		return users;
	}

}
