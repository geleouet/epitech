package epitech.twitter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;

public class Twitter {

	
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
		
		Javalin app = Javalin.create();
		
		List<User> users = new CopyOnWriteArrayList<>();
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
			}
			users.add(new User(login, idsUsers.getAndIncrement()));
		});
		app.get("/login/{login}", ctx -> {
			String login = ctx.pathParam("login");
			Optional<User> existing = users.stream().filter(u -> u.name.equals(login)).findAny();
			if (existing.isPresent()) {
				ctx.cookie("id", "" + existing.get().id);
			}
			else {
				throw new NotFoundResponse();
			}
		});
		app.get("/logout", ctx -> {
			ctx.cookie("id", null);
		});
		app.get("/whoami", ctx -> {
			String id = ctx.cookie("id");
			if (id == null) {
				ctx.status(401);
				return;
			}
			ctx.result(id);
		});
		
		
		app.start(port);
	}
}
