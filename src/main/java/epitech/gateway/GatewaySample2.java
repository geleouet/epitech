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

public class GatewaySample2 {

	
	public static void main(String[] args) throws IOException, InterruptedException {
		int portGateway = Integer.parseInt(System.getProperty("port", "9002"));
		int portWebServerSample = Integer.parseInt(System.getProperty("port", "9001"));

		
		HttpClient httpClient = HttpClient.newBuilder()
				.build();
		
		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		
		{
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + portGateway + "/register/panier"))
					.POST(BodyPublishers.ofString("http://localhost:" + portWebServerSample + "/fruits"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		{
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + portGateway + "/panier"))
					.POST(BodyPublishers.ofString("{\"name\": \"cerise\"}"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		{
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + portGateway + "/panier/"))
					.build();
			HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(send.statusCode());
			System.out.println(send.body());
		}
		
	}
}
