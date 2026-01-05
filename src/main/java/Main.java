import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

public class Main {

    public static String dbUri = "mongodb://localhost:27017";
    public static Preferences LocalStorage = Preferences.userNodeForPackage(Main.class);

    public static void listUpcomingEvents(Document session, MongoDatabase db) {
        Scanner sc = new Scanner(System.in);

        MongoCollection<Document> events = db.getCollection("events");
        MongoCollection<Document> users = db.getCollection("users");

        Object userId = session.get("user-id");
        Document user = users.find(new Document("_id", userId)).first();

        ArrayList<Object> eventsList = new ArrayList<>(user.getList("upcoming-events", Object.class));

        int choice = -1;
        do {
            if (eventsList.isEmpty()) {
                System.out.println("You have no upcoming events :(");
                break;
            }
            System.out.println("[-1] exit");
            for (int i = 0; i < eventsList.size(); i++) {
                Object eventId = eventsList.get(i);
                Document event = events.find(new Document("_id", eventId)).first();
                String title = event.getString("title");
                String location = event.getString("location");
                long dateTimeInSeconds = event.getLong("date-time");
                String dateTimeString = Instant.ofEpochSecond(dateTimeInSeconds).toString();

                Object adminId = event.get("admin");
                Document admin = users.find(new Document("_id", adminId)).first();

                System.out.printf("[%d] %s - %s - %s - %s\n", i, title, admin.getString("username"), dateTimeString,
                        location);

                ArrayList<Object> participants = new ArrayList<>(event.getList("participancts", Object.class));
                System.out.print("    - Participants:");
                for (Object participantId : participants) {
                    Document participant = users.find(new Document("_id", participantId)).first();
                    String participantUsername = participant.getString("username");
                    System.out.print(" " + participantUsername);
                }
                System.out.println();
            }

            System.out.print("which event you would like to cancel: ");
            choice = sc.nextInt();
            if (choice < -1 || choice >= eventsList.size()) {
                System.out.println("Invalid Entry");
                continue;
            }
            if (choice > -1) {
                Object eventId = eventsList.get(choice);
                users.findOneAndUpdate(new Document("_id", userId),
                        new Document("$pull", new Document("upcoming-events", eventId)));

                events.findOneAndUpdate(new Document("_id", eventId),
                        new Document("$pull", new Document("participancts", userId)));

                eventsList.remove(choice);
            } else {
                break;
            }

        } while (choice != -1);

        mainMenu();

    }

    public static void requests(Document session, MongoDatabase db) {

        Scanner sc = new Scanner(System.in);

        MongoCollection<Document> requests = db.getCollection("requests");
        MongoCollection<Document> events = db.getCollection("events");
        MongoCollection<Document> users = db.getCollection("users");

        Object userId = session.get("user-id");

        ArrayList<Document> requestsQuery = new ArrayList<>();
        requests.find(new Document("user", userId)).into(requestsQuery);

        if (requestsQuery.isEmpty()) {
            System.out.println("You have no requests :(");
            mainMenu();
        }

        for (int i = 0; i < requestsQuery.size(); i++) {
            Document request = requestsQuery.get(i);
            Object requestId = request.get("_id");
            Object eventId = request.get("event");
            Document event = events.find(new Document("_id", eventId)).first();
            String title = event.getString("title");
            String location = event.getString("location");
            long dateTimeInSeconds = event.getLong("date-time");
            String dateTimeString = Instant.ofEpochSecond(dateTimeInSeconds).toString();

            Object adminId = event.get("admin");
            Document admin = users.find(new Document("_id", adminId)).first();

            System.out.printf("[%d] %s - %s - %s - %s\n", i, title, admin.getString("username"), dateTimeString,
                    location);
            int choice = -1;
            while (true) {

                System.out.print("Accept (0) - Deny (1) - Not Sure (2): ");
                choice = sc.nextInt();

                switch (choice) {
                    case 0:
                        users.findOneAndUpdate(new Document("_id", userId),
                                new Document("$push", new Document("upcoming-events", eventId)));

                        requests.findOneAndDelete(new Document("_id", requestId));

                        users.findOneAndUpdate(new Document("_id", userId),
                                new Document("$pull", new Document("requests", requestId)));

                        events.findOneAndUpdate(new Document("_id", eventId),
                                new Document("$push", new Document("participancts", userId)));
                        break;

                    case 1:
                        requests.findOneAndDelete(new Document("_id", requestId));

                        users.findOneAndUpdate(new Document("_id", userId),
                                new Document("$pull", new Document("requests", requestId)));
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

    public static void sendRequest(MongoDatabase db, Object currentUserId, Document event) {
        Scanner sc = new Scanner(System.in);
        MongoCollection<Document> users = db.getCollection("users");
        ArrayList<Document> userQuery = new ArrayList<>();
        users.find().into(userQuery);

        int personIndex = 0;
        do {
            System.out.println("Please Choose The People want to invite");
            System.out.println("[-1] exit");

            for (int i = 0; i < userQuery.size(); i++) {
                Document user = userQuery.get(i);
                Object userId = user.get("_id");
                if (!userId.equals(currentUserId)) {
                    String username = user.getString("username");
                    System.out.printf("[%d] %s\n", i, username);
                } else {
                    userQuery.remove(i--);
                }
            }
            System.out.print("people you want to invite: ");
            personIndex = sc.nextInt();
            if (personIndex < -1 || personIndex >= userQuery.size()) {
                System.out.println("Invalid Entry");
                continue;
            }
            if (personIndex > -1) {
                Document userToInvite = userQuery.get(personIndex);
                MongoCollection<Document> requests = db.getCollection("requests");

                Object eventId = event.get("_id");
                Object invitedUserId = userToInvite.get("_id");
                Document newRequest = new Document("event", eventId)
                        .append("user", invitedUserId);

                requests.insertOne(newRequest);

                Object newRequestId = newRequest.get("_id");

                users.findOneAndUpdate(new Document("_id", invitedUserId),
                        new Document("$push", new Document("requests", newRequestId)));

                userQuery.remove(personIndex);

            } else {
                break;
            }

        } while (personIndex != -1);
    }

    public static void listMyEvents(Document session, MongoDatabase db) {
        Scanner sc = new Scanner(System.in);
        MongoCollection<Document> events = db.getCollection("events");
        MongoCollection<Document> users = db.getCollection("users");

        Object userId = session.get("user-id");
        ArrayList<Document> eventsQuery = new ArrayList<>();
        events.find(new Document("admin", userId)).into(eventsQuery);
        int choice = -1;
        do {
            System.out.println("Events Created By you: ");
            System.out.println("[-1] Exit");
            for (int i = 0; i < eventsQuery.size(); i++) {
                Document event = eventsQuery.get(i);
                String title = event.getString("title");
                String location = event.getString("location");
                long dateTimeInSeconds = event.getLong("date-time");
                String dateTimeString = Instant.ofEpochSecond(dateTimeInSeconds).toString();
                System.out.printf("[%d] %s - %s - %s\n", i, title, dateTimeString, location);

                ArrayList<Object> participants = new ArrayList<>(event.getList("participancts", Object.class));
                System.out.print("    - Participants:");
                for (Object participantId : participants) {
                    Document participant = users.find(new Document("_id", participantId)).first();
                    String participantUsername = participant.getString("username");
                    System.out.print(" " + participantUsername);
                }
                System.out.println();
            }
            System.out.print("event you want to edit: ");
            choice = sc.nextInt();
            if (choice < -1 || choice >= eventsQuery.size()) {
                System.out.println("Invalid Entry");
                continue;
            }
            if (choice > -1) {
                Document selectedEvent = eventsQuery.get(choice);
                String title = selectedEvent.getString("title");
                String location = selectedEvent.getString("location");
                long dateTimeInSeconds = selectedEvent.getLong("date-time");

                OffsetDateTime dateTime = Instant.ofEpochSecond(dateTimeInSeconds).atOffset(ZoneOffset.UTC);
                int year = dateTime.getYear();
                int month = dateTime.getMonthValue();
                int day = dateTime.getDayOfMonth();
                int hour = dateTime.getHour();
                int minute = dateTime.getMinute();

                sc.nextLine();
                System.out.println("press enter if want to keep it the same");
                System.out.printf("Old %s (%s): ", "title", title);
                String newTitle = sc.nextLine();
                System.out.printf("Old %s (%s): ", "location", location);
                String newLocation = sc.nextLine();

                title = newTitle.isEmpty() ? title : newTitle;
                newLocation = newLocation.isEmpty() ? location : newLocation;

                dateTimeInSeconds = 0;
                while (true) {
                    LocalDate today = LocalDate.now();
                    long timeNow = Instant.now().getEpochSecond();
                    System.out.printf("Old %s (%d): ", "year", year);
                    String newYear = sc.nextLine();
                    System.out.printf("Old %s (%d): ", "month", month);
                    String newMonth = sc.nextLine();
                    System.out.printf("Old %s (%d): ", "day", day);
                    String newDay = sc.nextLine();
                    System.out.printf("Old %s (%d): ", "hour", hour);
                    String newHour = sc.nextLine();
                    System.out.printf("Old %s (%d): ", "minutes", minute);
                    String newMinute = sc.nextLine();

                    year = newYear.isEmpty() ? year : Integer.parseInt(newYear);
                    month = newMonth.isEmpty() ? month : Integer.parseInt(newMonth);
                    day = newDay.isEmpty() ? day : Integer.parseInt(newDay);
                    hour = newHour.isEmpty() ? hour : Integer.parseInt(newHour);
                    minute = newMinute.isEmpty() ? minute : Integer.parseInt(newMinute);

                    String dateString = String.format("%04d-%02d-%02dT%02d:%02d:24.00Z", year, month, day, hour,
                            minute);


                    System.out.println(dateString);

                    try {
                        dateTimeInSeconds = Instant.parse(dateString)
                                .getEpochSecond();
                    } catch (DateTimeParseException e) {
                        System.out.println("Invalid Time Or Date Please try again");
                        continue;
                    }

                    if (dateTimeInSeconds >= timeNow) {
                        break;

                    } else {
                        System.out.println("Please Enter a Time and date that's >= " + today);
                    }
                }

                Object eventId = selectedEvent.get("_id");
                events.findOneAndUpdate(new Document("_id", eventId),
                        new Document("$set", new Document("title", title)
                                .append("location", location)
                                .append("date-time", dateTimeInSeconds)));

                sendRequest(db, userId, selectedEvent);
            } else {
                break;
            }

        } while (choice != -1);

        mainMenu();

    }

    public static void createEvent(Document session, MongoDatabase db) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter a event title: ");
        String eventTitle = sc.nextLine();

        long dateTimeInSeconds = 0;
        while (true) {
            LocalDate today = LocalDate.now();
            long timeNow = Instant.now().getEpochSecond();

            System.out.println("Enter Date and Time Accepted >= " + today);

            System.out.print("Enter The Year (Accepted 20XX): ");
            int year = sc.nextInt();

            System.out.print("Enter The Month (Accepted 1 - 12): ");
            int month = sc.nextInt();

            System.out.print("Enter The Day (Accepted: 1 - 31): ");
            int day = sc.nextInt();

            System.out.print("Enter The Hour (Accepted: 0 - 24): ");
            int hour = sc.nextInt();

            System.out.print("Enter The minutes (Accepted: 0 - 60): ");
            int minutes = sc.nextInt();

            String dateString = String.format("%04d-%02d-%02dT%02d:%02d:24.00Z", year, month, day, hour, minutes);

            System.out.println(dateString);

            try {
                dateTimeInSeconds = Instant.parse(dateString)
                        .getEpochSecond();
            } catch (DateTimeParseException e) {
                System.out.println("Invalid Time Or Data Please try again");
                continue;
            }

            if (dateTimeInSeconds >= timeNow) {
                break;

            } else {
                System.out.println("Please Enter a Time and date thats >= " + today);
            }
        }

        sc.nextLine(); // So that location won't be empty
        System.out.print("Enter a location: ");
        String eventLocation = sc.nextLine();

        MongoCollection<Document> events = db.getCollection("events");

        Object currentUserId = session.get("user-id");

        Document newEvent = new Document("title", eventTitle)
                .append("location", eventLocation)
                .append("date-time", dateTimeInSeconds)
                .append("admin", currentUserId)
                .append("participancts", Arrays.asList(currentUserId));

        events.insertOne(newEvent);

        sendRequest(db, currentUserId, newEvent);

        mainMenu();
    }

    public static void createUser() {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter a username: ");
        String userName = sc.nextLine();

        System.out.print("Enter a password: ");
        String password = sc.nextLine();

        System.out.print("confirm the password: ");
        String confirmPassword = sc.nextLine();

        if (!password.equals(confirmPassword)) {
            System.out.println("Password Confirmation is wrong please try again");
            createUser();
        }

        try (MongoClient mongoClient = MongoClients.create(dbUri)) {

            MongoDatabase db = mongoClient.getDatabase("event-manager");
            MongoCollection<Document> users = db.getCollection("users");

            Document existingUser = users.find(new Document("username", userName)).first();

            if (existingUser == null) {

                Document newUser = new Document("username", userName)
                        .append("password", password);

                users.insertOne(newUser);

                signin(newUser, db);
                mainMenu();

            } else {
                System.out.println("User already exists");
                ArrayList<String> options = new ArrayList<>();
                options.add("Try Again");
                options.add("Sign in instead");
                options.add("Back to main");

                int choice = getUserChoice(options);
                switch (choice) {
                    case 0:
                        createUser();
                        break;

                    case 1:
                        signin(db);
                        break;

                    case 2:
                        mainMenu();
                        break;

                    default:
                        break;
                }
            }

        }

    }

    public static void signin(Document user, MongoDatabase db) {
        MongoCollection<Document> storage = db.getCollection("storage");

        long secondsSinceEpoch = Instant.now().getEpochSecond();

        String username = user.getString("username");
        Object userId = user.get("_id");

        Document session = new Document("username", username)
                .append("time-created", secondsSinceEpoch)
                .append("user-id", userId);

        storage.insertOne(session);

        LocalStorage.put("session", session.get("_id").toString());
    }

    public static void signin(MongoDatabase db) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter a username: ");
        String userName = sc.nextLine();

        System.out.print("Enter a password: ");
        String password = sc.nextLine();

        MongoCollection<Document> users = db.getCollection("users");

        Document user = users.find(new Document("username", userName)).first();
        if (user != null) {
            String truePassword = user.getString("password");
            if (password.equals(truePassword)) {

                signin(user, db);
                mainMenu();

            } else {

                System.out.println("password doesn't match");
                ArrayList<String> options = new ArrayList<>();
                options.add("Try Again");
                options.add("Create a user instead");
                options.add("Back to main");

                int choice = getUserChoice(options);
                switch (choice) {
                    case 0:
                        signin(db);
                        break;

                    case 1:
                        createUser();
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

            int choice = getUserChoice(options);
            switch (choice) {
                case 0:
                    signin(db);
                    break;

                case 1:
                    createUser();
                    break;

                case 2:
                    mainMenu();
                    break;

                default:
                    break;
            }
        }

    }

    public static int getUserChoice(ArrayList<String> options) {
        Scanner sc = new Scanner(System.in);
        int i = 0;

        for (; i < options.size(); i++) {
            System.out.printf("[%d] %s\n", i, options.get(i));
        }

        System.out.print("Your Choice: ");
        int choice = sc.nextInt();

        if (choice >= i) {
            System.out.println("Please enter a valid Entry");
            return getUserChoice(options);
        }
        return choice;
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

    public static void mainMenu() {
        Scanner sc = new Scanner(System.in);

        try (MongoClient mongoClient = MongoClients.create(dbUri)) {
            String sessionId = LocalStorage.get("session", null);
            MongoDatabase db = mongoClient.getDatabase("event-manager");

            if (sessionId == null) {
                ArrayList<String> options = new ArrayList<>();
                options.add("Create a user");
                options.add("Sign in");
                options.add("Exit");

                int choice = getUserChoice(options);

                switch (choice) {
                    case 0:
                        createUser();
                        break;

                    case 1:
                        signin(db);

                    default:
                        break;
                }

            } else {

                Object objId = new ObjectId(sessionId);

                MongoCollection<Document> storage = db.getCollection("storage");

                Document session = storage.find(new Document("_id", objId)).first();
                long time = session.getLong("time-created");
                long secondsSinceEpoch = Instant.now().getEpochSecond();
                long duration = secondsSinceEpoch - time;
                long oneDayInSeconds = TimeUnit.DAYS.toSeconds(1);

                if (duration > oneDayInSeconds) {
                    logout(session, db);
                }

                String username = session.getString("username");
                System.out.println("Hello " + username + "! :)");

                ArrayList<String> options = new ArrayList<>();
                options.add("Create an Event");
                options.add("List My Events");
                options.add("List Upcoming Events");
                options.add("Requests");
                options.add("Log out");
                options.add("Exit");

                int choice = getUserChoice(options);

                switch (choice) {
                    case 0:
                        createEvent(session, db);
                        break;

                    case 1:
                        listMyEvents(session, db);
                        break;

                    case 2:
                        listUpcomingEvents(session, db);
                        break;

                    case 3:
                        requests(session, db);
                        break;

                    case 4:
                        logout(session, db);
                        break;
                    case 5:
                        break;

                    default:
                        System.out.println("Invalid Entry");
                        mainMenu();
                        break;
                }

            }

        }
    }

    public static void main(String[] args) {
        mainMenu();
    }
}
