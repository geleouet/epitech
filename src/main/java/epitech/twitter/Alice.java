package epitech.twitter;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
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

import epitech.twitter.WorkerService.Post;

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
	
	public static class Post {
		public String idPost;
		public Instant creationDate;
		public Integer idAuthor;
		public String message;

		public Post() {
		}

		public Post(String idPost, Instant timestamp, Integer idAuthor, String message) {
			super();
			this.idPost = idPost;
			this.creationDate = timestamp;
			this.idAuthor = idAuthor;
			this.message = message;
		}

		public String getIdPost() {
			return idPost;
		}

		public void setIdPost(String idPost) {
			this.idPost = idPost;
		}

		public Instant getCreationDate() {
			return creationDate;
		}

		public void setCreationDate(Instant creationDate) {
			this.creationDate = creationDate;
		}

		public Integer getIdAuthor() {
			return idAuthor;
		}

		public void setIdAuthor(Integer idAuthor) {
			this.idAuthor = idAuthor;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		int port = Integer.parseInt(System.getProperty("port", "9021"));
		String path = "/twitter";
		System.out.println("Starting Alice with port = " + port);

		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();

		
		HttpClient httpClient = HttpClient.newBuilder()
				.cookieHandler(cookieManager) // cookies are necessary for session handling
				.build();
		{

			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path + "/register/Alice"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		{
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path +  "/login/Alice"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		{
			String influencerName = "Bob";
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path + "/members"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
			List<User> users = new ObjectMapper().readValue(send.body(), new TypeReference<List<User>>(){});
			User influencer = users.stream().filter(u -> influencerName.equals(u.name)).findAny().get();
			HttpRequest requestFollowBob = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path + "/follow/"+influencer.id))
					.build();
			HttpResponse<String> sendFollowBob = httpClient.send(requestFollowBob, BodyHandlers.ofString());
			System.out.println(sendFollowBob.statusCode());
			System.out.println(sendFollowBob.body());
		}
		
		{
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port +path+ "/history"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
			List<Post> posts = mapper.readValue(send.body(), new TypeReference<List<Post>>(){});
			for (Post p : posts) {
				System.out.println(p.idAuthor);
				System.out.println(p.creationDate);
				System.out.println(p.message);
				System.out.println();
			}
		}
		
		
	
	}
}
