package epitech.distributedkeyvalue;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;

import com.fasterxml.jackson.databind.ObjectMapper;

public class NodeKeyValueClientSample {

	
	static class Message {
		public String message;

		public Message() {
		}
		
		public Message(String message) {
			super();
			this.message = message;
		}
		
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		int port = Integer.parseInt(System.getProperty("port", "9231"));

		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		
		HttpClient httpClient = HttpClient.newBuilder()
				.followRedirects(Redirect.NORMAL)
				.cookieHandler(cookieManager) // cookies are necessary for session handling
				.build();
		{
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/put/hello"))
					.POST(BodyPublishers.ofString("{\"message\": \"hello\"}"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		{

			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/get/hello"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		{
			Message message = new Message("world");
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/put/world"))
					.POST(BodyPublishers.ofString(new ObjectMapper().writeValueAsString(message)))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		{
			
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/get/world"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
			Message message = new ObjectMapper().readValue(send.body(), Message.class);
			System.out.println(message.message);
		}

	}
}
