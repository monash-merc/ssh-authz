package au.org.massive.oauth2_hpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Utility class to process simple key/value json data in the request body. Does not support depth greater than 1
 * @author jrigby
 *
 */
public class JsonRequest {
	private static final Logger log = Logger.getLogger(JsonRequest.class.getName());
	
	private JsonRequest() {
		
	}
	
	/**
	 * Extracts the request body from {@link HttpServletRequest}
	 * @param request the requst from which to extract the json request
	 * @return string representation of the request body
	 */
	private static String getRequestBody(HttpServletRequest request) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line+"\n");
		}
		return sb.toString();
	}
	
	/**
	 * Converts a simple key/value json request to a <pre>Map&lt;String,Object&gt;</pre> object
	 * @param request the requst from which to extract the json request
	 * @return a Map of the key/value data
	 * @throws JsonSyntaxException if the request contains invalid json
	 */
	@SuppressWarnings("unchecked")
	public static Map<String,Object> processJsonRequest(HttpServletRequest request) throws JsonSyntaxException {
		Gson gson = new Gson();
		try {
			String requestBody = getRequestBody(request);
			log.debug("Decoding request body: "+requestBody);
			return gson.fromJson(requestBody, HashMap.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
