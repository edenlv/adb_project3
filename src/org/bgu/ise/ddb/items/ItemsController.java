/**
 * 
 */
package org.bgu.ise.ddb.items;

import java.io.IOException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.File;

import javax.servlet.http.HttpServletResponse;

import org.bgu.ise.ddb.MediaItems;
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



/**
 * @author Alex
 *
 */
@RestController
@RequestMapping(value = "/items")
public class ItemsController extends ParentController {

	private Connection conn = null;

	/**
	 * Aux method for code reuse
	 * @return 
	 */
	private MongoCollection<Document> getItemsCollection(){
		return ParentController.getDB().getCollection("MediaItems");
	}

	/**
	 * Auxiliary method for connection to oracle DB
	 * @return
	 */
	private Connection getOracleConnection() {
		if (conn!=null) return conn;

		final String connectionUrl = "jdbc:oracle:thin:@ora1.ise.bgu.ac.il:1521/ORACLE";
		final String username = "razyid";
		final String password = "abcd";
		final String driver = "oracle.jdbc.driver.OracleDriver";

		try {

			Class.forName(driver); //registration of the driver
			conn = DriverManager.getConnection(connectionUrl, username, password);
			conn.setAutoCommit(false);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return conn;

	}

	/**
	 * The function copy all the items(title and production year) from the Oracle table MediaItems to the System storage.
	 * The Oracle table and data should be used from the previous assignment
	 */
	@RequestMapping(value = "fill_media_items", method={RequestMethod.GET})
	public void fillMediaItems(HttpServletResponse response){
		System.out.println("was here");

		List<Document> ans = getMediaItemsFromSQL();

		HttpStatus status = HttpStatus.OK;
		
		if (ans.size() > 0) getItemsCollection().insertMany(ans);
		else status = HttpStatus.CONFLICT;
		
		response.setStatus(status.value());
	}
	
	private List<Document> getMediaItemsFromSQL(){
		Connection conn = getOracleConnection();

		if (conn!=null) System.out.println("got oracle connection");

		List<Document> ans = new ArrayList<Document>();

		PreparedStatement ps = null;
		String query = "select title, prod_year from MediaItems";
		try {
			ps = conn.prepareStatement(query);
			ResultSet rs=ps.executeQuery();
			while(rs.next()){
				ans.add(new Document("title", rs.getString("title")).append("prod_year", rs.getInt("prod_year")));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();

		} finally {
			try {
				if (ps != null) ps.close();
			} catch (SQLException sqle) {
				sqle.printStackTrace();
			}
		}
		
		return ans;
	}

	/**
	 * The function copy all the items from the remote file,
	 * the remote file have the same structure as the films file from the previous assignment.
	 * You can assume that the address protocol is http
	 * @throws IOException 
	 */
	@RequestMapping(value = "fill_media_items_from_url", method={RequestMethod.GET})
	public void fillMediaItemsFromUrl(@RequestParam("url")    String urladdress,
			HttpServletResponse response) throws IOException{
		System.out.println(urladdress);

		File f = File.createTempFile("media", null);
		Files.copy(new URL(urladdress).openStream(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);

		List<Document> items = readItems(f);
		
		HttpStatus status = HttpStatus.OK;
		if (items.size() > 0) getItemsCollection().insertMany(items);
		else status = HttpStatus.CONFLICT;
		
		response.setStatus(status.value());
	}

	/**
	 * Helper method to parse CSV file into media items objects
	 * @param f
	 * @return
	 */
	private List<Document> readItems(File f) {
		List<Document> result = new ArrayList<Document>();

		try (Scanner scanner = new Scanner(f)) {

			while(scanner.hasNextLine()){
				String[] str = scanner.nextLine().split(",");
				Document d = new Document("title", str[0]).append("prod_year", Integer.parseInt(str[1]));
				result.add(d);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * The function retrieves from the system storage N items,
	 * order is not important( any N items) 
	 * @param topN - how many items to retrieve
	 * @return
	 */
	@RequestMapping(value = "get_topn_items",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(MediaItems.class)
	public MediaItems[] getTopNItems(@RequestParam("topn")    int topN){

		List<MediaItems> result = new ArrayList<MediaItems>();

		getItemsCollection().find().limit(topN).forEach(
				(Document doc) -> {
					result.add(new MediaItems(doc.getString("title"), doc.getInteger("prod_year")));
				});

		MediaItems[] items = new MediaItems[result.size()];
		result.toArray(items);

		return items;
	}


}
