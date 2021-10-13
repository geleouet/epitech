package epitech.bus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import epitech.bus.Bus.BusMessage;

public class BusClient {

	private final String topic;
	private final BusClientConfig busClientConfig;

	public static BusClientConfigBuilder builder() {
		return new BusClientConfigBuilder();
	}
	
	public void subscribe(Consumer<String> callback) {
		busClientConfig.add(callback, topic);
	}

	public void send(String message) {
		busClientConfig.send(topic, message);
	}

	public void close() {
		busClientConfig.close(topic);
	}

	private BusClient(BusClientConfig busClientConfig, String topic) {
		this.busClientConfig = busClientConfig;
		this.topic = topic;
	}

	
	public static class BusClientConfigBuilder {
		int port = 9207;
		static ConcurrentMap<Integer, BusClientConfig> byPort = new ConcurrentHashMap<>();
		
		public BusClientConfigBuilder port(int port) {
			this.port = port;
			return this;
		}

		public BusClientConfig create() {
			return byPort.computeIfAbsent(port, BusClientConfig::new);
		}
		
	}
	
	static class BusClientConfig {
		
		private int port;
		
		private BusClientConfig(int port) {
			super();
			this.port = port;
		}

		private void close(String topic) {
			Optional.ofNullable(webSockets.get(topic)).ifPresent(ws -> ws.sendClose(WebSocket.NORMAL_CLOSURE, "ok"));			
		}

		private void send(String topic, String message) {
			var ws = webSockets.computeIfAbsent(topic, this::connectToWebSocket);
			ws.sendText(message, true);			
		}

		private void add(Consumer<String> callback, String topic) {
			listeners.computeIfAbsent(topic, this::subscribeToBus).add(callback);			
		}

		public BusClient topic(String topic) {
			return new BusClient(this, topic);
		}
		
		private HttpClient client = HttpClient.newHttpClient();;
		private ConcurrentMap<String, List<Consumer<String>>> listeners = new ConcurrentHashMap<>();
		private ConcurrentMap<String, WebSocket> webSockets = new ConcurrentHashMap<>();

		private List<Consumer<String>> subscribeToBus(String topic) {
			connectToWebSocket(topic);
			return new CopyOnWriteArrayList<>();
		}

		// https://golb.hplar.ch/2019/01/java-11-http-client.html
		private WebSocket connectToWebSocket(String topic) {
			Listener wsListener = createListener(topic);
			WebSocket webSocket = client.newWebSocketBuilder()
					.buildAsync(URI.create("ws://localhost:" + port + "/topic/" + topic), wsListener).join();
			webSockets.put(topic, webSocket);
			return webSocket;
		}

		private Listener createListener(String topic) {
			return new Listener() {
				@Override
				public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
					try {
						BusMessage readValue = new ObjectMapper().readValue(data.toString(), BusMessage.class);
						listeners.getOrDefault(topic, Collections.emptyList())
								.forEach(c -> c.accept(readValue.message));

					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
					System.out.println("[" + topic + "] on : " + data);
					return Listener.super.onText(webSocket, data, last);
				}

				@Override
				public void onOpen(WebSocket webSocket) {
					System.out.println("[" + topic + "] onOpen");
					Listener.super.onOpen(webSocket);
				}

				@Override
				public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
					System.out.println("[" + topic + "] onClose " + statusCode + " " + reason);
					return Listener.super.onClose(webSocket, statusCode, reason);
				}
			};
		}

	}

	

}
