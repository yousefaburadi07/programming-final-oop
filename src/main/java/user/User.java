package user;

import com.mongodb.client.MongoDatabase;

import mongo.Mongo;
import requests.Request;

import com.mongodb.client.MongoCollection;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static main.Main.LocalStorage;
import static main.Main.mainMenu;
import events.Event;

public class User extends Mongo {

    private String username;
    private String password;
    private ArrayList<Object> requests;
    private ArrayList<Object> upcomingEvents;

    public void signin(MongoDatabase db) {
        Session session = new Session(getUsername(), get_id());
        session.create(db);
    }

    public User(MongoDatabase db, Object _id) {
        super(null, db);

        MongoCollection<Document> users = db.getCollection("users");
        Document user = users.find(new Document("_id", _id)).first();

        setUsername(user.getString("username"));
        setPassword(user.getString("password"));
        List<Object> reqList = user.getList("requests", Object.class);
        setRequests(reqList != null ? new ArrayList<>(reqList) : new ArrayList<>());

        List<Object> eventList = user.getList("upcoming-events", Object.class);
        setUpcomingEvents(eventList != null ? new ArrayList<>(eventList) : new ArrayList<>());
        set_id(user.get("_id"));
    }

    public User(MongoDatabase db, String username, String password, ArrayList<Object> requests,
            ArrayList<Object> upcomingEvents) {
        super(null, db);

        setUsername(username);
        setPassword(password);
        setRequests(requests);
        setUpcomingEvents(upcomingEvents);

        MongoCollection<Document> users = db.getCollection("users");

        Document newUser = new Document("username", getUsername())
                .append("password", getPassword())
                .append("requests", new ArrayList<>())
                .append("upcoming-events", new ArrayList<>());

        users.insertOne(newUser);

        set_id(newUser.get("_id"));
    }

    public static ArrayList<User> getAllUsers(MongoDatabase db) {

        MongoCollection<Document> users = db.getCollection("users");
        ArrayList<Document> userQuery = new ArrayList<>();
        users.find().into(userQuery);

        ArrayList<User> result = new ArrayList<>();
        for (int i = 0; i < userQuery.size(); i++) {
            Object _id = userQuery.get(i).get("_id");
            User user = new User(db, _id);

            result.add(user);
        }

        return result;

    }

    public ArrayList<Event> getEvents(MongoDatabase db) {

        MongoCollection<Document> events = db.getCollection("events");
        ArrayList<Document> eventsQuery = new ArrayList<>();
        events.find(new Document("admin", get_id())).into(eventsQuery);

        ArrayList<Event> result = new ArrayList<>();
        for (int i = 0; i < eventsQuery.size(); i++) {
            Document eventQuery = eventsQuery.get(i);
            Event event = new Event(db, eventQuery.get("_id"));

            System.out.print("get all ");
            System.out.println(event.get_id());

            result.add(event);
        }

        return result;
    }

    public ArrayList<Event> getUpcomingEventsAsObjects() {
        ArrayList<Object> eventsIds = getUpcomingEvents();
        ArrayList<Event> result = new ArrayList<>();

        for (int i = 0; i < eventsIds.size(); i++) {
            Event event = new Event(getDb(), eventsIds.get(i));
            result.add(event);
        }

        return result;
    }

    public void setUsername(String username) {
        if (get_id() != null) {
            MongoDatabase db = getDb();
            MongoCollection<Document> users = db.getCollection("users");
            users.findOneAndUpdate(new Document("_id", get_id()), new Document("username", username));
        }

        this.username = username;
    }

    public void setPassword(String password) {
        if (get_id() != null) {
            MongoDatabase db = getDb();
            MongoCollection<Document> users = db.getCollection("users");
            users.findOneAndUpdate(new Document("_id", get_id()),
                    new Document("$set", new Document("password", password)));
        }
        this.password = password;
    }

    public void setRequests(ArrayList<Object> requests) {
        if (get_id() != null) {
            MongoDatabase db = getDb();
            MongoCollection<Document> users = db.getCollection("users");
            users.findOneAndUpdate(new Document("_id", get_id()),
                    new Document("$set", new Document("requests", requests)));
        }
        this.requests = requests;
    }

    public void setUpcomingEvents(ArrayList<Object> upcomingEvents) {
        if (get_id() != null) {
            MongoDatabase db = getDb();
            MongoCollection<Document> users = db.getCollection("users");
            users.findOneAndUpdate(new Document("_id", get_id()),
                    new Document("$set", new Document("upcoming-events", upcomingEvents)));
        }
        this.upcomingEvents = upcomingEvents;
    }

    public void addRequest(Object request) {
        if (get_id() != null) {
            MongoDatabase db = getDb();
            MongoCollection<Document> users = db.getCollection("users");
            users.findOneAndUpdate(
                    new Document("_id", get_id()),
                    new Document("$push", new Document("requests", request)));
        }

        if (this.requests == null) {
            this.requests = new ArrayList<>();
        }
        this.requests.add(request);
    }

    public void removeRequest(Object request) {
        if (get_id() != null) {
            MongoCollection<Document> users = getDb().getCollection("users");
            users.findOneAndUpdate(
                    new Document("_id", get_id()),
                    new Document("$pull", new Document("requests", request)));
        }

        if (this.requests != null) {
            this.requests.remove(request);
        }
    }

    public void addUpcomingEvent(Object upcomingEvent) {
        System.out.println("someting ");
        System.out.println(upcomingEvent);

        System.out.println("someting2j ");
        System.out.println(get_id());
        if (get_id() != null) {
            System.out.println("this is workinm ");
            MongoCollection<Document> users = getDb().getCollection("users");
            users.findOneAndUpdate(
                    new Document("_id", get_id()),
                    new Document("$push", new Document("upcoming-events", upcomingEvent)));
        }

        if (this.upcomingEvents == null) {
            this.upcomingEvents = new ArrayList<>();
        }
        this.upcomingEvents.add(upcomingEvent);
    }

    public void removeUpcomingEvent(Object upcomingEvent) {
        if (get_id() != null) {
            MongoCollection<Document> users = getDb().getCollection("users");
            users.findOneAndUpdate(
                    new Document("_id", get_id()),
                    new Document("$pull", new Document("upcoming-events", upcomingEvent)));
        }

        if (this.upcomingEvents != null) {
            this.upcomingEvents.remove(upcomingEvent);
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public ArrayList<Object> getRequests() {
        return requests;
    }

    public ArrayList<Request> getRequestsAsObjects() {

        ArrayList<Request> result = new ArrayList<>();
        System.out.println(requests);

        if (requests != null) {

            if (requests.isEmpty())
                return result;

            for (int i = 0; i < requests.size(); i++) {
                Object requestId = requests.get(i);
                Request request = new Request(getDb(), requestId);

                result.add(request);
            }
        }

        return result;
    }

    public ArrayList<Object> getUpcomingEvents() {
        return upcomingEvents;
    }

    public void create(MongoDatabase db) {
        if (get_id() == null) {
            MongoCollection<Document> users = db.getCollection("users");

            Document newUser = new Document("username", getUsername())
                    .append("password", getPassword());

            users.insertOne(newUser);

            set_id(newUser.get("_id"));
        } else {
            System.out.println("this is already created");

            mainMenu();
        }
    }

    public static void createUserMenu(MongoDatabase db) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter a username: ");
        String userName = sc.nextLine();

        System.out.print("Enter a password: ");
        String password = sc.nextLine();

        System.out.print("confirm the password: ");
        String confirmPassword = sc.nextLine();

        if (!password.equals(confirmPassword)) {
            System.out.println("Password Confirmation is wrong please try again");
            createUserMenu(db);
        }

        MongoCollection<Document> users = db.getCollection("users");

        Document existingUser = users.find(new Document("username", userName)).first();

        if (existingUser == null) {

            User user = new User(db, userName, password, null, null);

            user.signin(db);

            mainMenu();

        } else {
            System.out.println("User already exists");

            mainMenu();
        }

    }

    public static void signinMenu(MongoDatabase db) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter a username: ");
        String userName = sc.nextLine();

        System.out.print("Enter a password: ");
        String password = sc.nextLine();

        MongoCollection<Document> users = db.getCollection("users");

        Document userDocument = users.find(new Document("username", userName)).first();
        if (userDocument != null) {
            String truePassword = userDocument.getString("password");
            if (password.equals(truePassword)) {
                User user = new User(db, userDocument.get("_id"));

                user.signin(db);
                mainMenu();

            } else {

                System.out.println("password doesn't match");
                ArrayList<String> options = new ArrayList<>();
                options.add("Try Again");
                options.add("Create a user instead");
                options.add("Back to main");

                int choice = sc.nextInt();
                switch (choice) {
                    case 0:
                        signinMenu(db);
                        break;

                    case 1:
                        createUserMenu(db);
                        break;

                    case 2:
                        mainMenu();
                        break;

                    default:
                        break;

                }

            }

        } else {

            System.out.println("user doesn't exit");

            ArrayList<String> options = new ArrayList<>();
            options.add("Try Again");
            options.add("Create a user instead");
            options.add("Back to main");

            int choice = sc.nextInt();
            switch (choice) {
                case 0:
                    signinMenu(db);
                    break;

                case 1:
                    createUserMenu(db);
                    break;

                case 2:
                    mainMenu();
                    break;

                default:
                    break;
            }
        }

    }

    public static void logout(Document session, MongoDatabase db) {
        MongoCollection<Document> storage = db.getCollection("storage");

        Object id = session.getObjectId("_id");
        if (id != null) {
            storage.deleteOne(new Document("_id", id));
            LocalStorage.remove("session");
        }

        mainMenu();

    }
}
