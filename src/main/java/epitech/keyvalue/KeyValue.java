package epitech.keyvalue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;

public class KeyValue {

	public static void main(String[] args) {
		int port = Integer.parseInt(System.getProperty("port", "9217"));
		
		Javalin app = Javalin.create();
		
		ConcurrentMap<String, String> map = new ConcurrentHashMap<>();
		app.post("/put/{key}", ctx -> {
			map.put(ctx.pathParam("key"), ctx.body());
		});
		
		app.get("/get/{key}", ctx -> {
			String value = map.get(ctx.pathParam("key"));
			if (value == null) {
				throw new NotFoundResponse();
			}
			ctx.result(value);
		});

		
		app.delete("/delte/{key}", ctx -> {
			map.remove(ctx.pathParam("key"));
		});

		app.start(port);
		
	}
	
	
}
