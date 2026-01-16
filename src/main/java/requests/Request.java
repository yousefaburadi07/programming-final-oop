package requests;

import com.mongodb.client.MongoDatabase;

import mongo.Mongo;

import com.mongodb.client.MongoCollection;

import org.bson.BsonTimestamp;
import org.bson.Document;

import events.Event;
import java.util.ArrayList;
import java.util.Scanner;

import java.time.Instant;

import static user.User.getAllUsers;
import user.User;

import static main.Main.mainMenu;

public class Request extends Mongo {
    private Object eventId;
    private Object userId;

    public Request(MongoDatabase db, Object eventId, User user) {
        super(null, db);
        MongoCollection<Document> requests = db.getCollection("requests");

        Document newRequest = new Document("event", eventId)
                .append("user", user.get_id());

        requests.insertOne(newRequest);

        Object newRequestId = newRequest.get("_id");

        user.addRequest(newRequestId);

        setEventId(eventId);
        setUserId(user.get_id());
        set_id(newRequestId);
    }

    public Request(MongoDatabase db, Object _id) {
        super(null, db);
        MongoCollection<Document> requests = db.getCollection("requests");

        Document request = requests.find(new Document("_id", _id)).first();

        setEventId(request.get("event"));
        setUserId(request.get("user"));
        set_id(_id);
    }

    public void removeRequest() {
        if (get_id() != null) {
            User user = new User(getDb(), getUserId());
            user.removeRequest(get_id());

            MongoCollection<Document> requests = getDb().getCollection("requests");
            requests.findOneAndDelete(new Document("_id", get_id()));
        }
    }

    public static void sendRequest(MongoDatabase db, Object currentUserId, Event event) {
        Scanner sc = new Scanner(System.in);

        ArrayList<User> users = new ArrayList<>(getAllUsers(db));

        int personIndex = 0;
        do {
            System.out.println("Please Choose The People want to invite");
            System.out.println("[-1] exit");

            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                Object userId = user.get_id();
                if (!userId.equals(currentUserId)) {
                    String username = user.getUsername();
                    System.out.printf("[%d] %s\n", i, username);
                } else {
                    users.remove(i--);
                }
            }

            System.out.print("people you want to invite: ");
            personIndex = sc.nextInt();
            if (personIndex < -1 || personIndex >= users.size()) {
                System.out.println("Invalid Entry");
                continue;
            }
            if (personIndex > -1) {
                User userToInvite = users.get(personIndex);
                Object eventId = event.get_id();

                new Request(db, eventId, userToInvite);
                users.remove(personIndex);

            } else {
                break;
            }

        } while (personIndex != -1);
    }

    public static void requests(Document session, MongoDatabase db) {

        Scanner sc = new Scanner(System.in);

        Object userId = session.get("user-id");
        User user = new User(db, userId);

        ArrayList<Request> requests = user.getRequestsAsObjects();

        if (requests.isEmpty()) {
            System.out.println("You have no requests :(");
            mainMenu();
        }

        for (int i = 0; i < requests.size(); i++) {
            Request request = requests.get(i);
            Object eventId = request.getEventId();

            Event event = new Event(db, eventId);

            String title = event.getTitle();
            String location = event.getLocation();
            long dateTimeInSeconds = event.getDateTime();
            String dateTimeString = Instant.ofEpochSecond(dateTimeInSeconds).toString();

            Object adminId = event.getAdmin();
            User admin = new User(db, adminId);

            System.out.printf("[%d] %s - %s - %s - %s\n", i, title, admin.getUsername(), dateTimeString,
                    location);
            int choice = -1;
            while (true) {

                System.out.print("Accept (0) - Deny (1) - Not Sure (2): ");
                choice = sc.nextInt();

                switch (choice) {
                    case 0:
                        user.addUpcomingEvent(eventId);
                        request.removeRequest();
                        event.addParticipant(userId);
                        break;

                    case 1:
                        request.removeRequest();
                        break;
                    case 2:
                        break;
                    default:
                        continue;

                }
                break;

            }

        }
        mainMenu();

    }

    private void setUserId(Object userId) {
        if (get_id() != null) {
            MongoCollection<Document> collection = getDb().getCollection("requests");
            collection.findOneAndUpdate(new Document("_id", get_id()), new Document("$set", new Document("user", userId)));
        }
        this.userId = userId;
    }

    private void setEventId(Object eventId) {
        if (get_id() != null) {
            MongoCollection<Document> collection = getDb().getCollection("requests");
            collection.findOneAndUpdate(new Document("_id", get_id()), new Document("$set", new Document("event", eventId)));
        }
        this.eventId = eventId;
    }

    public Object getUserId() {
        return userId;
    }

    public Object getEventId() {
        return eventId;
    }

}
