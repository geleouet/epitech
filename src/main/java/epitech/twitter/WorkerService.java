package epitech.twitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import epitech.bus.BusQueueClient;
import epitech.logs.LogApi;
import io.javalin.http.HttpCode;

public class WorkerService {

	private static LogApi log;
	
	static class NewPostMessage {
		public Post message;
		public Integer timeline;
	}

	public static class Post {
		public String idPost;
		public Instant creationDate;
		public Integer idAuthor;
		public String message;

		public Post() {
		}

		public Post(String idPost, Instant timestamp, Integer idAuthor, String message) {
			super();
			this.idPost = idPost;
			this.creationDate = timestamp;
			this.idAuthor = idAuthor;
			this.message = message;
		}

		public String getIdPost() {
			return idPost;
		}

		public void setIdPost(String idPost) {
			this.idPost = idPost;
		}

		public Instant getCreationDate() {
			return creationDate;
		}

		public void setCreationDate(Instant creationDate) {
			this.creationDate = creationDate;
		}

		public Integer getIdAuthor() {
			return idAuthor;
		}

		public void setIdAuthor(Integer idAuthor) {
			this.idAuthor = idAuthor;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

	}
	
	public static void main(String[] args) throws InterruptedException {
		UUID idProcess = UUID.randomUUID();

		System.out.println("Starting worker " + idProcess);
		int portBus = Integer.parseInt(System.getProperty("portBus", "9207"));
		int portKVS = Integer.parseInt(System.getProperty("portKeyValueStore", "9231"));
		log = new LogApi("Worker["+idProcess+"]", portBus);

		HttpClient httpClient = HttpClient.newBuilder()
				.build();

		var queue = BusQueueClient.builder()
				.port(portBus)
				.create()
				.queue("worker");
		
		log.info("Worker "+idProcess+" started");
		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
		
		queue.subscribe(x -> {
			log.info("Receive work");
			try {
				NewPostMessage post = mapper.readValue(x, NewPostMessage.class);
				
				{
					log.info("build timeline for " + post.timeline);
					HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + portKVS + "/get/timeline_" + post.timeline))
							.build();
					HttpResponse<String> send = httpClient.send(request, BodyHandlers.ofString());
					
					List<Post> current = new ArrayList<>();
					if (send.statusCode() == HttpCode.OK.getStatus()) {
						current = mapper.readValue(send.body(), new TypeReference<List<Post>>(){});
					}
					current.add(post.message);
					
					current.sort(Comparator.comparing(Post::getCreationDate).reversed());
					if (current.size() > 10) {
						current = current.subList(0, 10);
					}
					
					{
						HttpRequest requestPut = HttpRequest.newBuilder(URI.create("http://localhost:" + portKVS + "/put/timeline_" + post.timeline))
								.POST(BodyPublishers.ofString(mapper.writeValueAsString(current)))
								.build();
						httpClient.send(requestPut, BodyHandlers.ofString());
						log.info("timeline for " + post.timeline + " updated");
					}
				}
				
				
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				log.info("Cannot deserialize (" + e.getMessage() +")");
				return false;
			}
			System.out.println("OK " + x);
			return true;
		});
		
		Thread.sleep(Integer.MAX_VALUE);
	}
}