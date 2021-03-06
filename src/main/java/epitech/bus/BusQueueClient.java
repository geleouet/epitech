package epitech.bus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import epitech.bus.Bus.BusMessage;

public class BusQueueClient {

	static boolean log = false;
	
	public static BusClientConfigBuilder builder() {
		return new BusClientConfigBuilder();
	}

	public static class Queue {

		private final String channel;
		private final BusClientConfig busClientConfig;

		public void subscribe(Predicate<String> callback) {
			busClientConfig.add(callback, channel);
		}

		public void send(String message) {
			busClientConfig.send(channel, message);
		}

		public void close() {
			busClientConfig.close(channel);
		}

		private Queue(BusClientConfig busClientConfig, String channel) {
			this.busClientConfig = busClientConfig;
			this.channel = channel;
		}
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

	public static class BusClientConfig {

		private int port;
		ObjectMapper mapper = new ObjectMapper();

		private BusClientConfig(int port) {
			super();
			this.port = port;
		}


		private void close(String channel) {
			Optional.ofNullable(webSockets.get(channel)).ifPresent(ws -> ws.sendClose(WebSocket.NORMAL_CLOSURE, "ok"));
		}

		private void send(String channel, String message) {
			var ws = webSockets.computeIfAbsent(channel, this::connectToWebSocket);
			try {
				ws.sendText(mapper.writeValueAsString(new Bus.QueueMessage("MSG", message)), true);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}

		private void add(Predicate<String> callback, String queue) {
			listeners.computeIfAbsent(queue, this::subscribeToBus).set(callback);
		}

		public Queue queue(String queue) {
			return new Queue(this, queue);
		}

		private class Wrapper {
			Predicate<String> t;

			public Wrapper(Predicate<String> t) {
				super();
				this.t = t;
			}
			
			public void set(Predicate<String> t) {
				this.t = t != null ? t : __ -> false;
			}

			public void apply(Consumer<Wrapper> c) {
				c.accept(this);
			}

			public boolean test(String message) {
				return t.test(message);
			}
		}
		private HttpClient client = HttpClient.newHttpClient();;
		private ConcurrentMap<String, Wrapper> listeners = new ConcurrentHashMap<>();
		private ConcurrentMap<String, WebSocket> webSockets = new ConcurrentHashMap<>();

		private Wrapper subscribeToBus(String queue) {
			connectToWebSocket(queue);
			return new Wrapper(__ -> false);
		}

		// https://golb.hplar.ch/2019/01/java-11-http-client.html
		private WebSocket connectToWebSocket(String queue) {
			Listener wsListener = createListener(queue);
			WebSocket webSocket = client.newWebSocketBuilder().buildAsync(URI.create("ws://localhost:" + port + "/queue/" + queue), wsListener).join();
			return webSocket;
		}

		private Listener createListener(String queue) {
			return new Listener() {

				List<CharSequence> parts = new ArrayList<>();
				 
				@Override
				public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
					if (log) {
						System.out.println("[" + queue + "] on : " + data);
					}
					parts.add(data);
		            
					if (last) {
						String message = parts.stream().collect(Collectors.joining());
						parts.clear();

						try {
							BusMessage readValue = mapper.readValue(message.toString(), BusMessage.class);
							listeners.getOrDefault(queue, new Wrapper(__ -> false)).apply(c -> {
								boolean test = c.test(readValue.message);
								try {
									if (test) {
										webSocket.sendText(mapper.writeValueAsString(new Bus.QueueMessage("ACK", readValue.message)), true);
									}
									else {
										webSocket.sendText(mapper.writeValueAsString(new Bus.QueueMessage("NACK", readValue.message)), true);
									}
								} catch (JsonProcessingException e) {
									e.printStackTrace();
								}
							});

						} catch (JsonProcessingException e) {
							e.printStackTrace();
						}
					}
					return Listener.super.onText(webSocket, data, last);
				}

				@Override
				public void onOpen(WebSocket webSocket) {
					System.out.println("[" + queue + "] onOpen");
					Listener.super.onOpen(webSocket);
				}

				@Override
				public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
					System.out.println("[" + queue + "] onClose " + statusCode + " " + reason);
					return Listener.super.onClose(webSocket, statusCode, reason);
				}
			};
		}

	}

}
