
package main;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import com.mongodb.client.MongoCollection;

import static user.User.createUserMenu;
import static user.User.signinMenu;
import static user.User.logout;

import static requests.Request.requests;

import static events.Event.createEvent;
import static events.Event.listMyEvents;
import static events.Event.listUpcomingEvents;;


public class Main {

    public static String dbUri = "mongodb://3.85.108.161:27017/";
    public static Preferences LocalStorage = Preferences.userNodeForPackage(Main.class);



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
                        createUserMenu(db);
                        break;

                    case 1:
                        signinMenu(db);

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
