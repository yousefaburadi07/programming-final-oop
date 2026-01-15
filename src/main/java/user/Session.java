package user;

import java.time.Instant;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import static main.Main.LocalStorage;

public class Session {
    private Object _id;
    private String username;
    private Object userId;
    private long timeCreated;

    public Session(String username, Object userId) {
        setUsername(username);
        setUserId(userId);
        setTimeCreated();

    }

    public void set_id(Object _id) {
        this._id = _id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserId(Object userId) {
        this.userId = userId;
    }

    public void setTimeCreated() {
        long secondsSinceEpoch = Instant.now().getEpochSecond();
        this.timeCreated = secondsSinceEpoch;
    }

    public Object get_id() {
        return _id;
    }

    public String getUsername() {
        return username;
    }

    public Object getUserId() {
        return userId;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void create(MongoDatabase db) {

        MongoCollection<Document> storage = db.getCollection("storage");

        Document session = new Document("username", getUsername())
                .append("time-created", getTimeCreated())
                .append("user-id", getUserId());

        storage.insertOne(session);

        LocalStorage.put("session", session.get("_id").toString());
    }

}
