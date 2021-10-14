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
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Bob {

	static DateTimeFormatter formatter =
		    DateTimeFormatter.ofLocalizedDateTime( FormatStyle.FULL )
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
	
	public static void main(String[] args) throws IOException, InterruptedException {
		int port = Integer.parseInt(System.getProperty("port", "9021"));
		String path = "/twitter";
		System.out.println("Starting Bob with port = " + port);

		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		
		HttpClient httpClient = HttpClient.newBuilder()
				.cookieHandler(cookieManager) // cookies are necessary for session handling
				.build();
		{

			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path + "/register/Bob"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		{
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path + "/login/Bob"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		
		while (true)
		{
			Tweet tweet = new Tweet("Il est " + formatter.format(Instant.now()));
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path + "/postTweet"))
					.POST(BodyPublishers.ofString(new ObjectMapper().writeValueAsString(tweet)))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
			Thread.sleep(10_000);
		}
	}
}
