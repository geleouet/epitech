package epitech.distributedkeyvalue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.json.JavalinJackson;

public class NodeKeyValue {

	
	static class Shares {
		public List<Share> shares = new ArrayList<>();
	}
	
	static class Share {
		public List<Integer> values;
		public Integer id;
		public Share() {
		}
		public Share(Integer id) {
			super();
			this.id = id;
			this.values = new ArrayList<>();
		}
		
	}
	
	static Set<Integer> aliveNodes = ConcurrentHashMap.newKeySet();
	static ConcurrentHashMap<Integer, Instant> lastSeen = new ConcurrentHashMap<>();
	
	static Shares shares = new Shares();
	static int pingInterval = 10;
	static int moduloGeneral = 10;
	static int replica = 2;
	static int port;
	static int portReference;
	static boolean logPing = true;

	static ConcurrentMap<String, String> map = new ConcurrentHashMap<>();
	
	static HttpClient httpClient = HttpClient.newBuilder()
			.build();

	public static void main(String[] args) {
		port = Integer.parseInt(System.getProperty("port", "9231"));
		portReference = Integer.parseInt(System.getProperty("portReference", "9231"));
		

		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();

		Javalin app = Javalin.create(config -> {
			config.jsonMapper(new JavalinJackson(mapper));
		});

		// Autofind a free port
		while (true) {
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/info"))
					.build();
			try {
				httpClient.send(request, BodyHandlers.ofString());
				port ++;
			} catch (IOException | InterruptedException e) {
				break;
			}
		}
		
		rebalance();
		
		app.get("/info", ctx -> {
			ctx.result("OK");
		});
		app.get("/shares", ctx -> {
			ctx.json(shares);
		});
		app.get("/ping/{adresse}", ctx -> {
			Integer adress = ctx.pathParamAsClass("adresse", Integer.class).get();
			if (logPing) System.out.println("Ping <= "+ adress);
			lastSeen.put(adress, Instant.now());
			ctx.json(shares);
		});
		
		
		app.post("/put/{key}", ctx -> {
			String key = ctx.pathParam("key");
			int resp = findHost(key);
			if (isHost(key)) {
				System.out.println("Put [" + key + "]"+hash(key)+" <= "+ctx.body());
				map.put(key, ctx.body());
				putExtern(resp, key, ctx.body());
			}
			else {
				String nextUrl = "http://localhost:"+resp+"/put/"+key;
				System.out.println("Redirect [" + key + "]"+hash(key)+" to "+nextUrl);
				ctx.redirect(nextUrl, 307);
			}
		});
		// Only accessible for replication /  distribution
		app.post("/putIntern/{key}", ctx -> {
			String key = ctx.pathParam("key");
			if (isHost(key)) {
				System.out.println("PutIntern [" + key + "]"+hash(key)+" <= "+ctx.body());
				map.put(key, ctx.body());
			}
		});
		
		app.get("/datas", ctx -> {
			ctx.json(map);
		});
		app.get("/get/{key}", ctx -> {
			String key = ctx.pathParam("key");
			int resp = findHost(key);
			if (isHost(key)) {
				System.out.println("Get [" + key + "]"+hash(key));
				String value = map.get(ctx.pathParam("key"));
				if (value == null) {
					throw new NotFoundResponse();
				}
				ctx.result(value);
			}
			else {
				String nextUrl = "http://localhost:"+resp+"/get/"+key;
				System.out.println("Redirect [" + key + "]"+hash(key)+" to "+nextUrl);
				ctx.redirect(nextUrl);
			}
			
		});
		
		app.get("/delete/{key}", ctx -> { // should be a delete
			map.remove(ctx.pathParam("key"));
		});

		app.start(port);
		
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(() -> ping(portReference), 0, pingInterval, TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(() -> alive(), 1, pingInterval, TimeUnit.SECONDS);
	}

	private static boolean isHost(String key) {
		int mod = hash(key);
		return shares.shares.stream().filter(s -> s.id == port).findAny().map(s -> s.values.contains(mod)).orElse(false);
	}

	private static int hash(String key) {
		return key.hashCode() % moduloGeneral;
	}

	private static int findHost(String key) {
		int mod = hash(key);
		return shares.shares.stream()
				.filter(s -> s.id != port)
				.filter(s -> s.values.contains(mod)).map(s -> s.id).findAny().orElse(portReference);
	}
	
	private static void alive() {
		boolean rebalance = false;
		List<Integer> toDelete = lastSeen.entrySet().stream()
				.filter(e -> !isStillAlive(e.getValue()))
				.map(Entry::getKey)
		.collect(Collectors.toList());
		rebalance |= toDelete.size() > 0;
		if (!toDelete.isEmpty()) {
			toDelete.forEach(i -> System.out.println("No more accessible : " + i));
			aliveNodes.removeAll(toDelete);
		}

		var toAdd = lastSeen.entrySet().stream()
				.filter(e -> isStillAlive(e.getValue()))
				.filter(e -> !aliveNodes.contains(e.getKey()))
				.map(Entry::getKey)
				.collect(Collectors.toList());
		rebalance |= toAdd.size() > 0;
		aliveNodes.addAll(toAdd);
		if (!toAdd.isEmpty()) {
			toAdd.forEach(i -> System.out.println("New node accessible : " + i));
		}
		
		if (rebalance) {
			rebalance();
		}
	}

	private static boolean isStillAlive(Instant value) {
		boolean check = value.isAfter(Instant.now().minus(2l * pingInterval, ChronoUnit.SECONDS));
		return check;
	}

	private static void rebalance() {
		System.out.println("Rebalance");
		ArrayList<Integer> nodes = new ArrayList<>(aliveNodes);
		nodes.add(port);
		Collections.shuffle(nodes);
		ConcurrentHashMap<Integer, Share> build = new ConcurrentHashMap<>();	
		for (int i = 0; i < 10; i++) {
			build.computeIfAbsent(nodes.get(i % nodes.size()), Share::new).values.add(i);
			build.computeIfAbsent(nodes.get(i % nodes.size()), Share::new).values.add((i+1) % moduloGeneral);
		}
		
		shares.shares = new ArrayList<>(build.values());
		new Thread(() -> relocate(), "Relocate").start();
	}

	private static void ping(int portReference) {
		if (port == portReference) return;
		if (logPing) System.out.println("Ping =>"+portReference);
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + portReference + "/ping/" + port))
				.build();
		HttpResponse<String> send;
		try {
			send = httpClient.send(request, BodyHandlers.ofString());
			Optional<Share> currentShare = shares.shares.stream().filter(s -> s.id == port).findAny();
			shares = new ObjectMapper().readValue(send.body(), Shares.class);
			Optional<Share> newShare = shares.shares.stream().filter(s -> s.id == port).findAny();
			if (newShare.isPresent()) {
				if (currentShare.isPresent()) {
					currentShare.get().values.removeAll(newShare.get().values);
					if (!currentShare.get().values.isEmpty()) {
						new Thread(() -> relocate(), "Relocate").start();				
					}
				}
			}
		} catch (IOException | InterruptedException e) {
			// Cannot join reference
			// start election
			System.exit(-1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void relocate() {
		System.out.println("Relocate datas");
		Set<String> keysToDelete = new HashSet<>();
		map.entrySet().stream()
		.filter(e -> !isHost(e.getKey()))
		.forEach(e -> {
			keysToDelete.add(e.getKey());
			int newHost = findHost(e.getKey());
			System.out.println("Move ["+e.getKey()+"] to " + newHost);
			put(newHost, e.getKey(), e.getValue());
		});
		keysToDelete.forEach(k -> map.remove(k));
	}

	private static void putExtern(int port, String key, String val) {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/putIntern/" + key))
				.POST(BodyPublishers.ofString(val))
				.build();
		try {
			httpClient.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	private static void put(int port, String key, String val) {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/putIntern/" + key))
				.POST(BodyPublishers.ofString(val))
				.build();
		try {
			httpClient.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
