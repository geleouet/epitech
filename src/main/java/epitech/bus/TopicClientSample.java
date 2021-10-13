package epitech.bus;

import java.util.concurrent.TimeUnit;

public class TopicClientSample {

	
	public static void main(String[] args) {

		var client = BusTopicClient.builder()
				.port(9207)
				.create()
				.topic("hello");
		client.subscribe(System.out::println);
		client.send("world");

		try {
			TimeUnit.SECONDS.sleep(30);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		client.close();

	}
}
