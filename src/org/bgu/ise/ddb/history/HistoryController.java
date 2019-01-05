/**
 * 
 */
package org.bgu.ise.ddb.history;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import com.mongodb.client.model.Sorts;

import jdk.nashorn.internal.ir.SetSplitState;

/**
 * @author Alex
 *
 */
@RestController
@RequestMapping(value = "/history")
public class HistoryController extends ParentController{

	/**
	 * Aux method for code reuse
	 * @return 
	 */
	private MongoCollection<Document> getHistoryCollection(){
		return ParentController.getDB().getCollection("History");
	}

 
	/**
	 * The function inserts to the system storage triple(s)(username, title, timestamp). 
	 * The timestamp - in ms since 1970
	 * Advice: better to insert the history into two structures( tables) in order to extract it fast one with the key - username, another with the key - title
	 * @param username
	 * @param title
	 * @param response
	 */
	@RequestMapping(value = "insert_to_history", method={RequestMethod.GET})
	public void insertToHistory (@RequestParam("username")    String username,
			@RequestParam("title")   String title,
			HttpServletResponse response){

		HttpStatus status = HttpStatus.OK;

		Document user = getDB().getCollection("Users").find(new Document("username", username)).first();
		Document item = getDB().getCollection("MediaItems").find(new Document("title", title)).first();

		if (user == null || item == null) {
			status = HttpStatus.CONFLICT;
		} else {
			Long curr_time = System.currentTimeMillis();
			Document doc = new Document("username", username);
			doc.append("title", title);
			doc.append("viewtime", curr_time);

			getHistoryCollection().insertOne(doc);
		}
		response.setStatus(status.value());

	}


	/**
	 * The function retrieves  users' history
	 * The function return array of pairs <title,viewtime> sorted by VIEWTIME in descending order
	 * @param username
	 * @return
	 */
	@RequestMapping(value = "get_history_by_users",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(HistoryPair.class)
	public HistoryPair[] getHistoryByUser(@RequestParam("entity")    String username){

		List<HistoryPair> h = new ArrayList<HistoryPair>();

		Document user = getDB().getCollection("Users").find(new Document("username", username)).first();
		if (user==null) {
			return new HistoryPair[0];
		} else {
			FindIterable<Document> result = getHistoryCollection().find(new Document("username", username)).sort(Sorts.descending("viewtime"));
			MongoCursor<Document> it = result.iterator();
			Document d = it.tryNext();

			while (d != null) {
				h.add(new HistoryPair(d.getString("title"), new Date(d.getLong("viewtime"))));
				d = it.tryNext();
			}
		}

		HistoryPair[] histories = new HistoryPair[h.size()];
		h.toArray(histories);

		return histories;
	}


	/**
	 * The function retrieves  items' history
	 * The function return array of pairs <username,viewtime> sorted by VIEWTIME in descending order
	 * @param title
	 * @return
	 */
	@RequestMapping(value = "get_history_by_items",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(HistoryPair.class)
	public  HistoryPair[] getHistoryByItems(@RequestParam("entity")    String title){

		List<HistoryPair> h = new ArrayList<HistoryPair>();

		Document media_title = getDB().getCollection("MediaItems").find(new Document("title", title)).first();
		if (media_title==null) {
			return new HistoryPair[0];
		} else {
			FindIterable<Document> result = getHistoryCollection().find(new Document("title", title)).sort(Sorts.descending("viewtime"));
			MongoCursor<Document> it = result.iterator();
			Document d = it.tryNext();

			while (d != null) {
				h.add(new HistoryPair(d.getString("username"), new Date(d.getLong("viewtime"))));
				d = it.tryNext();
			}
		}

		HistoryPair[] histories = new HistoryPair[h.size()];
		h.toArray(histories);

		return histories;
	}

	/**
	 * The function retrieves all the  users that have viewed the given item
	 * @param title
	 * @return
	 */
	@RequestMapping(value = "get_users_by_item",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(HistoryPair.class)
	public User[] getUsersByItem(@RequestParam("title") String title){

		List<User> aux = new ArrayList<User>();

		Document media_title = getDB().getCollection("MediaItems").find(new Document("title", title)).first();
		if (media_title==null) {
			return new User[0];
		} else {
			
			FindIterable<Document> result = getHistoryCollection().find(new Document("title", title));
			MongoCursor<Document> it = result.iterator();
			
			MongoCollection<Document> usersCollection = getDB().getCollection("Users");
			Document d = it.tryNext();
			while (null!=d) {
				Document d_user = new Document("username", d.getString("username"));
				d_user = usersCollection.find(d_user).first();
				if (null==d_user) return new User[0];
				else aux.add(new User(d_user.getString("username"), d_user.getString("first_name"), d_user.getString("last_name")));
				d = it.tryNext();
			}
		
		}
		
		User[] users = new User[aux.size()];
		aux.toArray(users);
		return users;
	}

	/**
	 * The function calculates the similarity score using Jaccard similarity function:
	 *  sim(i,j) = |U(i) intersection U(j)|/|U(i) union U(j)|,
	 *  where U(i) is the set of usernames which exist in the history of the item i.
	 * @param title1
	 * @param title2
	 * @return
	 */
	@RequestMapping(value = "get_items_similarity",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	public double  getItemsSimilarity(@RequestParam("title1") String title1,
			@RequestParam("title2") String title2){

		double ret = 0.0;
		
		/**
		 * IMPORTANT: I override the User#hashCode() and User#equals() methods for correct use in sets!
		 * Private comparators of Set will now work as expected.
		 */
		
		Set<User> users1 = new HashSet<User>(Arrays.asList(getUsersByItem(title1)));
		Set<User> users2 = new HashSet<User>(Arrays.asList(getUsersByItem(title2)));
		
		if (users1.size() == 0 || users2.size() == 0) return ret;
		
		Set<User> union_set = new HashSet<User>(users1);
		union_set.addAll(users2);
		
		int union = union_set.size();
		
		users1.retainAll(users2);
		int intersection = users1.size();
		
		if (union == 0 || intersection == 0) return ret;
		
		ret = ((double)intersection / union);
		
		return ret;
	}


}
