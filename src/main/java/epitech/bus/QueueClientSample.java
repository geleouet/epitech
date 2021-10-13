package epitech.bus;

import java.util.concurrent.TimeUnit;

public class QueueClientSample {

	
	public static void main(String[] args) {

		int busPort = 9207;
		
		createOkConsumer(busPort);

		var client = BusQueueClient.builder()
				.port(busPort)
				.create()
				.queue("hello");

		client.send("HELLO");
		client.send("WORLD");
		client.send("HOW");
		client.send("ARE");
		client.send("YOU");
		client.send("?");
		
		try {
			TimeUnit.SECONDS.sleep(30);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		client.close();
	}

	public static void createOkConsumer(int busPort) {
		var client = BusQueueClient.builder()
				.port(busPort)
				.create()
				.queue("hello");
		client.subscribe(x -> {
			System.out.println("OK " + x);
			return true;
		});
	}

	
}
