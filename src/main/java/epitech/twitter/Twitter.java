package epitech.twitter;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

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
	}

	public static class Post {
		public String idPost;
		public Instant timestamp;
		public Integer idAuthor;
		public String message;

		public Post() {
		}

		public Post(String idPost, Instant timestamp, Integer idAuthor, String message) {
			super();
			this.idPost = idPost;
			this.timestamp = timestamp;
			this.idAuthor = idAuthor;
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

	public static void main(String[] args) {
		int port = Integer.parseInt(System.getProperty("port", "9001"));

		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
		
		Javalin app = Javalin.create(config -> {
			config.jsonMapper(new JavalinJackson(mapper));
		});

		List<User> users = new CopyOnWriteArrayList<>();
		List<Post> posts = new CopyOnWriteArrayList<>();
		List<Follow> follows = new CopyOnWriteArrayList<>();

		AtomicInteger idsUsers = new AtomicInteger();

		app.get("/members", ctx -> {
			ctx.json(users);
		});
		app.get("/register/{login}", ctx -> {
			String login = ctx.pathParam("login");
			Optional<User> existing = users.stream().filter(u -> u.name.equals(login)).findAny();
			if (existing.isPresent()) {
				ctx.status(400);
				ctx.result("Already existing");
			} else {
				users.add(new User(login, idsUsers.getAndIncrement()));
			}
		});
		app.get("/login/{login}", ctx -> {
			String login = ctx.pathParam("login");
			Optional<User> existing = users.stream().filter(u -> u.name.equals(login)).findAny();
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
			Integer id = Integer.parseInt(ctx.cookie("id"));
			Tweet tweet = ctx.bodyAsClass(Tweet.class);
			Optional<User> user = users.stream().filter(u -> u.id == id).findAny();
			if (user.isEmpty()) {
				throw new UnauthorizedResponse();
			}
			Post newPost = new Post(UUID.randomUUID().toString(), Instant.now(), id, tweet.message);
			posts.add(newPost);
			System.out.println(user.get().name + "=>" + tweet.message);
			ctx.json(newPost);
		});
		app.get("/follow/{idUser}", ctx -> {
			Integer id = Integer.parseInt(ctx.cookie("id"));
			Integer idUser = ctx.pathParamAsClass("idUser", Integer.class).get();
			Follow follow = new Follow(id, idUser);
		});
		app.get("/timeline", ctx -> {
			Integer id = Integer.parseInt(ctx.cookie("id"));
			List<Post> timeline = follows.stream().filter(f -> f.idUser == id).flatMap(f -> posts.stream().filter(p -> p.idAuthor == f.idFollowed)).sorted(Comparator.comparing(p -> p.timestamp)).limit(10).collect(Collectors.toList());
			ctx.json(timeline);
		});

		app.start(port);
	}
}
