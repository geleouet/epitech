package epitech.logs;

import epitech.bus.BusTopicClient;

public class LogHubService {
	
	
	public static void main(String[] args) throws InterruptedException {
		int portBus  = Integer.parseInt(System.getProperty("portBus", "9207"));
		var client = BusTopicClient.builder()
				.port(portBus)
				.create()
				.topic("logs");
		client.subscribe(System.out::println);
		
		Thread.sleep(Integer.MAX_VALUE);
	}

}
