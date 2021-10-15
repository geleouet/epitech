package epitech.twitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import epitech.database.Database;
import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.json.JavalinJackson;

public class LoginService {

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
		int port = Integer.parseInt(System.getProperty("port", "9005"));
		
		//int portGateway = Integer.parseInt(System.getProperty("portGateway", "9021"));
		//registerSelf(port, portGateway);

		Jdbi jdbi = Database.start("login");
		initDB(jdbi);

		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();

		Javalin app = Javalin.create(config -> {
			config.jsonMapper(new JavalinJackson(mapper));
		});

		app.get("/members", ctx -> {
			System.out.println(">> /members");
			ctx.json(users(jdbi));
		});
		app.get("/register/{login}", ctx -> {
			String login = ctx.pathParam("login");
			System.out.println(">> /register/" + login);
			Optional<User> existing = users(jdbi).stream().filter(u -> u.name.equals(login)).findAny();
			if (existing.isPresent()) {
				ctx.status(400);
				ctx.result("Already existing");
			} else {
				ctx.json(add(jdbi, login));
			}
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

	private static User add(Jdbi jdbi, String name) {
		return jdbi.withHandle(h -> h.createUpdate("INSERT INTO user (name) VALUES (:name)").bind("name", name).executeAndReturnGeneratedKeys("id").mapTo(User.class).one());
	}

	private static List<User> users(Jdbi jdbi) {
		List<User> users = jdbi.withHandle(h -> h.select("SELECT * FROM user").mapTo(User.class).list());
		return users;
	}

	private static void initDB(Jdbi jdbi) {
		jdbi.registerRowMapper(BeanMapper.factory(User.class));
		jdbi.withHandle(h -> h.execute("CREATE TABLE IF NOT EXISTS user ( " + "   id  NUMBER PRIMARY KEY AUTO_INCREMENT, " + "   name  VARCHAR(140) " + "   ) "));
	}
}
