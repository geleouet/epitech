package epitech.web;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WebClientSample {

	static class FruitDTO {
		public Integer id;
		public String name;
		public FruitDTO() { }
		@Override
		public String toString() {
			return id + "->" + name;
		}
		
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		int port = Integer.parseInt(System.getProperty("port", "9001"));
		System.out.println("Starting sample web client with port = " + port);

		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		
		HttpClient httpClient = HttpClient.newBuilder()
				.cookieHandler(cookieManager) // cookies are necessary for session handling
				.build();
		{

			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/fruits"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}

		{
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/fruits"))
					.POST(BodyPublishers.ofString("{\"name\": \"pomme\"}"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}

		{
			FruitDTO banane = new FruitDTO();
			banane.name = "banane";
			ObjectMapper mapper = new ObjectMapper();
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/fruits"))
					.POST(BodyPublishers.ofString(mapper.writeValueAsString(banane)))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			FruitDTO readValue = mapper.readValue(send.body(), FruitDTO.class);
			System.out.println(send.statusCode());
			System.out.println(send.body());
			System.out.println(readValue);
		}

		{

			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/fruits"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}

		
	}
}
