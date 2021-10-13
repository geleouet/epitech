package epitech.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.h2.tools.Server;
import org.jdbi.v3.core.Jdbi;

public class Database {

	
	private static Server serverDBWeb;
	private static Server serverDB;

	public static void main(String[] args) throws Exception {
		serverDBWeb = Server.createWebServer("-webPort", "9124", "-ifNotExists", "-trace").start();
		serverDB = Server.createTcpServer("-tcpPort", "9123", "-ifNotExists").start();
		Thread shutdownHook = new Thread(() -> {
			serverDBWeb.stop();
			serverDB.stop();
		});
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		System.out.println("Database started");
	}
	
	public static void cleanDatabase(String databaseUrl, String dbUser, String dbPassword) throws SQLException {
		Connection conn = DriverManager.getConnection(databaseUrl, dbUser, dbPassword);
		conn.createStatement().execute("DROP ALL OBJECTS DELETE FILES");
		conn.commit();
		conn.close();
	}
	
	public static Jdbi start(String baseName) {
		String databaseUrl = "jdbc:h2:tcp://localhost:9123/~/db/"+baseName+".db";
		String dbUser="";
		String dbPassword="";
		System.out.println("Open '"+databaseUrl+"'");
		return Jdbi.create(databaseUrl, dbUser, dbPassword);
	}
}
