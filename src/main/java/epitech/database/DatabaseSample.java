package epitech.database;

import java.util.List;
import java.util.UUID;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;

public class DatabaseSample {

	public static class User {
		int id;
		String name;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return id + " " + name;
		}

	}
	
	public static void main(String[] args) {
		Jdbi jdbi = Database.start("hello");
		
		jdbi.registerRowMapper(BeanMapper.factory(User.class));
		jdbi.withHandle(h -> h.execute(
				"CREATE TABLE IF NOT EXISTS user ( " + 
						"	id  NUMBER PRIMARY KEY AUTO_INCREMENT, " + 
						"	name  VARCHAR(140) " + 
						"	) "
				));
		
		jdbi.withHandle(h -> h.createUpdate("INSERT INTO user (name) VALUES (:name)")
				.bind("name", UUID.randomUUID().toString())
				.execute());
		
		List<User> users = jdbi.withHandle(h -> h.select("SELECT * FROM user")
				.mapTo(User.class)
				.list());
		
		users.forEach(u -> System.out.println(u));
	}
}
