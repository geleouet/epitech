package epitech.twitter;

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import epitech.database.Database;
import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.plugin.json.JavalinJackson;

public class Twitter {

	public static class Tweet {
		public String message;

		public Tweet() {
		}

		public Tweet(String message) {
			this.message = message;
		}
	}

	public static class Follow {
		public Integer idUser;
		public Integer idFollowed;

		public Follow() {
		}

		public Follow(Integer idUser, Integer idFollowed) {
			super();
			this.idUser = idUser;
			this.idFollowed = idFollowed;
		}

		public Integer getIdUser() {
			return idUser;
		}

		public void setIdUser(Integer idUser) {
			this.idUser = idUser;
		}

		public Integer getIdFollowed() {
			return idFollowed;
		}

		public void setIdFollowed(Integer idFollowed) {
			this.idFollowed = idFollowed;
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

	public static class User {
		public int id;
		public String name;

		public User() {
		}

		public User(String login, int id) {
			this.name = login;
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	public static void main(String[] args) throws IOException, InterruptedException {
		int port = Integer.parseInt(System.getProperty("port", "9001"));
		int portGateway = Integer.parseInt(System.getProperty("portGateway", "9021"));
		Jdbi jdbi = Database.start("twitter");
		initDB(jdbi);
		registerSelf(port, portGateway);

		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();

		Javalin app = Javalin.create(config -> {
			config.jsonMapper(new JavalinJackson(mapper));
		});

		app.get("/members", ctx -> {
			ctx.json(users(jdbi));
		});
		app.get("/register/{login}", ctx -> {
			String login = ctx.pathParam("login");
			Optional<User> existing = users(jdbi).stream().filter(u -> u.name.equals(login)).findAny();
			if (existing.isPresent()) {
				ctx.status(400);
				ctx.result("Already existing");
			} else {
				add(jdbi, login);
			}
		});
		app.get("/login/{login}", ctx -> {
			String login = ctx.pathParam("login");
			Optional<User> existing = users(jdbi).stream().filter(u -> u.name.equals(login)).findAny();
			if (existing.isPresent()) {
				ctx.cookie("id", "" + existing.get().id);
			} else {
				throw new NotFoundResponse();
			}
		});
		app.get("/logout", ctx -> {
			ctx.cookie("id", null);
		});
		app.get("/whoami", ctx -> {
			UUID.randomUUID().toString();
			String id = ctx.cookie("id");
			if (id == null) {
				ctx.status(401);
				return;
			}
			ctx.result(id);
		});
		app.post("/postTweet", ctx -> {
			ctx.cookieMap().entrySet().forEach(e -> System.out.println(e.getKey() + ":"+ e.getValue()));
			Integer id = Integer.parseInt(ctx.cookie("id"));
			Tweet tweet = ctx.bodyAsClass(Tweet.class);
			Optional<User> user = users(jdbi).stream().filter(u -> u.id == id).findAny();
			if (user.isEmpty()) {
				throw new UnauthorizedResponse();
			}
			Post newPost = add(jdbi, id, tweet.message);
			System.out.println(user.get().name + "=>" + tweet.message);
			ctx.json(newPost);
		});
		app.get("/follow/{idUser}", ctx -> {
			Integer id = Integer.parseInt(ctx.cookie("id"));
			Integer idUser = ctx.pathParamAsClass("idUser", Integer.class).get();
			Follow follow = new Follow(id, idUser);
			add(jdbi, follow);
		});
		app.get("/timeline", ctx -> {
			Integer id = Integer.parseInt(ctx.cookie("id"));
			List<Post> timeline = follows(jdbi).stream()
					.filter(f -> f.idUser == id)
					.flatMap(f -> posts(jdbi).stream().filter(p -> p.idAuthor == f.idFollowed))
					.sorted(reverseOrder(comparing(p -> p.creationDate)))
					.limit(10)
					.collect(toList());
			ctx.json(timeline);
		});

		app.start(port);
	}

	private static void registerSelf(int port, int portGateway) throws IOException, InterruptedException {
		HttpClient httpClient = HttpClient.newBuilder()
				.build();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + portGateway + "/register/twitter"))
				.POST(BodyPublishers.ofString("http://localhost:" + port+""))
				.build();
		HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
		System.out.println(send.statusCode());
		System.out.println(send.body());
	}

	private static void add(Jdbi jdbi, Follow follow) {
		jdbi.withHandle(h -> h.createUpdate("INSERT INTO follow (idUser, idFollowed) VALUES (:idUser, :idFollowed)").bind("idUser", follow.idUser).bind("idFollowed", follow.idFollowed).execute());
	}

	private static List<Follow> follows(Jdbi jdbi) {
		List<Follow> follows = jdbi.withHandle(h -> h.select("SELECT * FROM follow").mapTo(Follow.class).list());
		return follows;
	}

	private static Post add(Jdbi jdbi, Integer id, String message) {
		var uuid = UUID.randomUUID().toString();
		var creationDate = Instant.now();
		jdbi.withHandle(h -> h.createUpdate("INSERT INTO post " + " (idPost, idAuthor, message, creationDate) VALUES (:idPost, :idAuthor, :message, :creationDate)").bind("idPost", uuid).bind("idAuthor", id).bind("message", message)
				.bind("creationDate", creationDate).execute());
		return new Post(uuid, creationDate, id, message);
	}

	private static List<Post> posts(Jdbi jdbi) {
		List<Post> posts = jdbi.withHandle(h -> h.select("SELECT * FROM post").mapTo(Post.class).list());
		return posts;
	}

	private static User add(Jdbi jdbi, String name) {
		return jdbi.withHandle(h -> h.createUpdate("INSERT INTO user (name) VALUES (:name)").bind("name", name).executeAndReturnGeneratedKeys("id").mapTo(User.class).one());
	}

	private static List<User> users(Jdbi jdbi) {
		List<User> users = jdbi.withHandle(h -> h.select("SELECT * FROM user").mapTo(User.class).list());
		return users;
	}

	private static void initDB(Jdbi jdbi) {
		jdbi.registerRowMapper(BeanMapper.factory(User.class));
		jdbi.registerRowMapper(BeanMapper.factory(Post.class));
		jdbi.registerRowMapper(BeanMapper.factory(Follow.class));
		jdbi.withHandle(h -> h.execute("CREATE TABLE IF NOT EXISTS post ( " + "   idPost  VARCHAR(64) PRIMARY KEY, " + "   message  VARCHAR(140), " + "   creationdate  TIMESTAMP," + "   idAuthor  NUMBER" + "   ) "));

		jdbi.withHandle(h -> h.execute("CREATE TABLE IF NOT EXISTS follow ( " + "   idUser  NUMBER, " + "   idFollowed  NUMBER " + "   ) "));

		jdbi.withHandle(h -> h.execute("CREATE TABLE IF NOT EXISTS user ( " + "   id  NUMBER PRIMARY KEY AUTO_INCREMENT, " + "   name  VARCHAR(140) " + "   ) "));

	}
}
