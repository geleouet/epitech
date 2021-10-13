package epitech.gateway;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import io.javalin.Javalin;

public class GatewaySample {

	public static void main(String[] args) throws IOException, InterruptedException {
		int portGateway = Integer.parseInt(System.getProperty("port", "9002"));
		int port = Integer.parseInt(System.getProperty("port", "9005"));

		
		Javalin app = Javalin.create();

		HttpClient httpClient = HttpClient.newBuilder()
				.build();
		
		app.get("/world", ctx -> {
			ctx.result("Hello world!");
		});
		app.start(port);
		
		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		
		{
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + portGateway + "/register/hello"))
					.POST(BodyPublishers.ofString("http://localhost:" + port+""))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		{
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + portGateway + "/hello/world"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		
		app.stop();
	}
}
