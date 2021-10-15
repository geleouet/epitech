package epitech.twitter;

import epitech.bus.BusQueueClient;

public class WorkerService {

	public static void main(String[] args) {
		int busPort = 9207;

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