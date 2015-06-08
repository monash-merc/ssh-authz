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
 * Utility class to process simple key/value json data in the request body. Does not support depth > 1
 * @author jrigby
 *
 */
public class JsonRequest {
	private static Logger log = Logger.getLogger(JsonRequest.class.getName());
	
	private JsonRequest() {
		
	}
	
	/**
	 * Extracts the request body from {@link HttpServletRequest}
	 * @param request
	 * @return string representation of the request body
	 * @throws IOException
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
	 * Converts a simple key/value json request to a {@link Map<String,String>}
	 * @param request
	 * @return a Map of the key/value data
	 * @throws JsonSyntaxException
	 */
	@SuppressWarnings("unchecked")
	public static Map<String,String> processJsonRequest(HttpServletRequest request) throws JsonSyntaxException {
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
