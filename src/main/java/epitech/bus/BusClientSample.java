package epitech.bus;

import java.util.concurrent.TimeUnit;

public class BusClientSample {

	
	public static void main(String[] args) {

		var client = BusClient.builder()
				.port(9207)
				.create()
				.topic("hello");
		client.subscribe(m -> System.out.println(m));
		client.send("world");

		try {
			TimeUnit.SECONDS.sleep(30);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		client.close();

	}
}
