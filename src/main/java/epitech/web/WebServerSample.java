package epitech.web;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;

public class WebServerSample {

	
	public static class Fruit {
		public int id;
		public String name;
	}
	
	public static void main(String[] args) {
		int port = Integer.parseInt(System.getProperty("port", "9001"));
		
		Javalin app = Javalin.create();
		
		List<Fruit> fruits = new CopyOnWriteArrayList<>();
		AtomicInteger ids = new AtomicInteger();
		
		app.get("/fruits", ctx -> {
			ctx.json(fruits);
		});
		app.get("/fruits/{id}", ctx -> {
			Integer id = ctx.pathParamAsClass("id", Integer.class).get();
			Optional<Fruit> findAny = fruits.stream().filter(f -> f.id == id).findAny();
			ctx.json(findAny.orElseThrow(() -> new NotFoundResponse()));
		});
		app.post("/fruits", ctx -> {
			Fruit fruit = ctx.bodyAsClass(Fruit.class);
			fruit.id = ids.getAndIncrement();
			fruits.add(fruit);
			ctx.json(fruit);
		});
		app.start(port);
	}
}
