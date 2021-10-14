package epitech.twitter;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Alice {

	static DateTimeFormatter formatter =
		    DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT )
		                     .withLocale( Locale.FRANCE )
		                     .withZone( ZoneId.systemDefault() );
	public static class Tweet {
		public String message;
		public Tweet() {
		}
		public Tweet(String message) {
			this.message = message;
		}

	}
	
	public static class User {
		public int id;
		public String name;

		public User() {
		}

		public User(String login, int id) {
			this.name = login;
			this.id = id;
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		int port = Integer.parseInt(System.getProperty("port", "9001"));
		System.out.println("Starting Alice with port = " + port);

		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		
		HttpClient httpClient = HttpClient.newBuilder()
				.cookieHandler(cookieManager) // cookies are necessary for session handling
				.build();
		{

			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/register/Alice"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		{
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login/Alice"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		{
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/members"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
			List<User> users = new ObjectMapper().readValue(send.body(), new TypeReference<List<User>>(){});
			User bob = users.stream().filter(u -> "Bob".equals(u.name)).findAny().get();
			HttpRequest requestFollowBob = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/follow/"+bob.id))
					.build();
			HttpResponse<String> sendFollowBob = httpClient.send(requestFollowBob, BodyHandlers.ofString());
			System.out.println(sendFollowBob.statusCode());
			System.out.println(sendFollowBob.body());
		}
		
		{
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/timeline"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		
		
	
	}
}
