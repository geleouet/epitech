package epitech.bus;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

public class Bus {

	private static Map<String, Map<WsContext, String>> topicUserUsernameMap = new ConcurrentHashMap<>();
	private static Map<String, Map<WsContext, String>> queueUserUsernameMap = new ConcurrentHashMap<>();
	private static AtomicInteger nextUserNumber = new AtomicInteger(1); // Assign to username for next connecting user

	
	public static void main(String[] args) {
		int port = Integer.parseInt(System.getProperty("port", "9207"));
		
		Javalin app = Javalin.create();
		
		app.ws("/topic/{topic}", ws -> {
			ws.onConnect(ctx -> {
                String username = "User" + nextUserNumber.getAndIncrement();
                String topic = ctx.pathParam("topic");
                topicUserUsernameMap.computeIfAbsent(topic, __ -> new HashMap<>())
                	.put(ctx, username);
                System.out.println("Server " + (username + " joined the topic '"+topic+"'"));
            });
            ws.onClose(ctx -> {
            	String topic = ctx.pathParam("topic");
            	String username = topicUserUsernameMap.getOrDefault(topic, Collections.emptyMap()).get(ctx);
            	System.out.println("Server " + (username + " left the topic '"+topic+"'"));
            	
            	Map<WsContext, String> userMap = topicUserUsernameMap.computeIfAbsent(ctx.pathParam("topic"), __ -> new HashMap<>());
                userMap.remove(ctx);
            });
            ws.onMessage(ctx -> {
            	String topic = ctx.pathParam("topic");
            	broadcastMessage(topic, ctx.message());
            });
		});
		app.ws("/queue/{queue}", ws -> {
			ws.onConnect(ctx -> {
				String username = "User" + nextUserNumber.getAndIncrement();
				String queue = ctx.pathParam("queue");
				queueUserUsernameMap.computeIfAbsent(queue, __ -> new HashMap<>())
				.put(ctx, username);
				System.out.println("Server " + (username + " joined the queue '"+queue+"'"));
			});
			ws.onClose(ctx -> {
				String topic = ctx.pathParam("queue");
				String username = queueUserUsernameMap.getOrDefault(topic, Collections.emptyMap()).get(ctx);
				System.out.println("Server " + (username + " left the topic '"+topic+"'"));
				
				Map<WsContext, String> userMap = queueUserUsernameMap.computeIfAbsent(ctx.pathParam("queue"), __ -> new HashMap<>());
				userMap.remove(ctx);
			});
			ws.onMessage(ctx -> {
				String topic = ctx.pathParam("queue");
				QueueMessage queueMessage = ctx.messageAsClass(QueueMessage.class);
				
				if ("NACK".equals(queueMessage.type)) {
					// try another send
					queueMessage(topic, queueMessage.message);
				}
				else if ("ACK".equals(queueMessage.type)) {
					// message delivered
				}
				else if ("MSG".equals(queueMessage.type)) {
					queueMessage(topic, queueMessage.message);
				}
			});
		});
		app.start(port);
	}
	
	
	public static class BusMessage {
		public String message;
		public BusMessage() {
		}
		public BusMessage(String message) {
			super();
			this.message = message;
		}
	}
	public static class QueueMessage {
		public String message;
		public String type;
		
		public QueueMessage() {
		}
		public QueueMessage(String type, String message) {
			super();
			this.type = type;
			this.message = message;
		}
	}
	
	// Sends a message from one user to all users, along with a list of current usernames
    private static void broadcastMessage(String topic, String message) {
    	BusMessage busMessage = new BusMessage(message);
    	topicUserUsernameMap.getOrDefault(topic, Collections.emptyMap()).keySet().stream()
        .filter(ctx -> ctx.session.isOpen())
        .forEach(session -> session.send(busMessage));
    }

    private static void queueMessage(String queue, String message) {
    	List<WsContext> connected = queueUserUsernameMap.getOrDefault(queue, Collections.emptyMap()).keySet().stream()
        .filter(ctx -> ctx.session.isOpen())
        .collect(Collectors.toList());
    	
    	// random choice
    	Collections.shuffle(connected);
    	
    	if (connected.size() > 0) {
    		connected.get(0).send(new BusMessage(message));
    	}
    	
    }
}
