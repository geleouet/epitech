package epitech.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.javalin.Javalin;

public class Gateway {
	

	public static void main(String[] args) {
		int port = Integer.parseInt(System.getProperty("port", "9002"));

		Javalin app = Javalin.create();

		HttpClient httpClient = HttpClient.newBuilder()
				.build();
		ConcurrentMap<String, String> services = new ConcurrentHashMap<>();
		
		app.post("/register/{path}", ctx -> {
			services.put(ctx.pathParam("path"), ctx.body());
		});

		
		app.get("*", ctx -> {
			String path = ctx.path();
			String[] split = path.split("/");
			System.out.println(Arrays.toString(split));
			
			String service = split[1];
			String url = services.get(service);
			
			String remainingPath = path.substring(1 + service.length());
			System.out.println(remainingPath);
			String newUrl = url+remainingPath;
			HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(URI.create(newUrl)).build(), BodyHandlers.ofString());
			ctx.status(response.statusCode());
			if (response.body() != null) {
				ctx.result(response.body());
			}
		});
		app.post("*", ctx -> {
			String path = ctx.path();
			String[] split = path.split("/");
			System.out.println(Arrays.toString(split));
			
			String service = split[1];
			String url = services.get(service);
			
			String remainingPath = path.substring(1 + service.length());
			System.out.println(remainingPath);
			String newUrl = url+remainingPath;
			HttpResponse<String> response = httpClient.send(HttpRequest
					.newBuilder(URI.create(newUrl))
					.POST(BodyPublishers.ofString(ctx.body()))
					.build(), BodyHandlers.ofString());
			ctx.status(response.statusCode());
			if (response.body() != null) {
				ctx.result(response.body());
			}
		});
		app.start(port);
	}

}
