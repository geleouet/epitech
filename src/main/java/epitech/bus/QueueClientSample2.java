package epitech.bus;

import java.util.concurrent.TimeUnit;

import epitech.bus.BusQueueClient.Queue;

public class QueueClientSample2 {

	
	public static void main(String[] args) {

		int busPort = 9207;
		
		var client = createKoConsumer(busPort);
		
		try {
			TimeUnit.SECONDS.sleep(30);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		client.close();
	}
	
	public static Queue createKoConsumer(int busPort) {
		var client = BusQueueClient.builder()
				.port(busPort)
				.create()
				.queue("hello");
		client.subscribe(x -> {
			System.out.println("KO " + x);
			return false;
		});
		return client;
	}


}
