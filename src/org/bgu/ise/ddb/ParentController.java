/**
 * 
 */
package org.bgu.ise.ddb;

import java.util.HashSet;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClient;
import com.mongodb.Block;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.HandlerMapping;

/**
 * @author Alex
 *
 */
public class ParentController {

	/**
	 * Aux method for code reuse
	 */
	protected static MongoDatabase getDB() {
		MongoClient client = new MongoClient("localhost", 27017);
		MongoDatabase database = client.getDatabase("edendb");
		return database;
		
	}
	
	@ModelAttribute
	public void tagController(HttpServletRequest request) {
		Set<org.springframework.http.MediaType> supportedMediaTypes = new HashSet<>();
		supportedMediaTypes.add(MediaType.APPLICATION_JSON);
		request.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE,
				supportedMediaTypes);
	}

}
