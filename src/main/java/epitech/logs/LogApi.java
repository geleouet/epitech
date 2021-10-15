package epitech.logs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import epitech.bus.BusTopicClient;
import epitech.bus.BusTopicClient.Topic;

public class LogApi {

	private String service;
	private Topic client;
	private ObjectMapper mapper;

	public LogApi(String service, int busPort) {
		this.service = service;
		client = BusTopicClient.builder()
				.port(busPort)
				.create()
				.topic("logs");
		mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
	}

	public static class LogMessage {
		public String service;
		public String message;
		public LogMessage() {
		}
		public LogMessage(String service, String message) {
			super();
			this.service = service;
			this.message = message;
		}
	}
	
	public void send(String message) {
		try {
			client.send(mapper.writeValueAsString(new LogMessage(service, message)));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	public void info(String message) {
		send(message);
	}

	
	
	
}
