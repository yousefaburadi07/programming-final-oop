package events;

import org.bson.Document;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.time.LocalDate;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import static requests.Request.sendRequest;
import static main.Main.mainMenu;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import user.User;

import mongo.Mongo;

public class Event extends Mongo {

    private String title;
    private String location;
    private long dateTime;
    private Object admin;
    private ArrayList<Object> participants;

    public Event(MongoDatabase db, Object _id) {
        super(null, db);

        MongoCollection<Document> events = db.getCollection("events");
        Document eventDocument = events.find(new Document("_id", _id)).first();

        setTitle(eventDocument.getString("title"));
        setLocation(eventDocument.getString("location"));
        setDateTime(eventDocument.getLong("date-time"));
        setAdmin(eventDocument.get("admin"));
        List<Object> participantsList = eventDocument.getList("participants", Object.class);
        setParticipants(participantsList == null ? new ArrayList<Object>() : new ArrayList<>(participantsList));
        set_id(_id);
    }

    public Event(MongoDatabase db, String title, String location, long dateTime, Object admin,
            ArrayList<Object> participants) {
        super(null, db);

        MongoCollection<Document> events = db.getCollection("events");

        setDb(db);
        setTitle(title);
        setLocation(location);
        setDateTime(dateTime);
        setAdmin(admin);
        setParticipants(participants);

        Document event = new Document("title", title)
                .append("location", location)
                .append("date-time", dateTime)
                .append("admin", admin)
                .append("participants", participants);

        events.insertOne(event);

        System.out.print("event creation ");
        System.out.println(event.get("_id"));
        set_id(event.get("_id"));

        System.out.print("event setting ");
        System.out.println(get_id());
    }

    public void setTitle(String title) {
        if (get_id() != null) {
            MongoCollection<Document> collection = getDb().getCollection("events");
            collection.findOneAndUpdate(new Document("_id", get_id()),
                    new Document("$set", new Document("title", title)));
        }
        this.title = title;
    }

    public void setLocation(String location) {
        if (get_id() != null) {
            MongoCollection<Document> collection = getDb().getCollection("events");
            collection.findOneAndUpdate(new Document("_id", get_id()),
                    new Document("$set", new Document("location", location)));
        }
        this.location = location;
    }

    public void setDateTime(long dateTime) {
        if (get_id() != null) {
            MongoCollection<Document> collection = getDb().getCollection("events");
            collection.findOneAndUpdate(new Document("_id", get_id()),
                    new Document("$set", new Document("date-time", dateTime)));
        }
        this.dateTime = dateTime;
    }

    public void setAdmin(Object admin) {
        if (get_id() != null) {
            MongoCollection<Document> collection = getDb().getCollection("events");
            collection.findOneAndUpdate(new Document("_id", get_id()),
                    new Document("$set", new Document("admin", admin)));
        }
        this.admin = admin;
    }

    public void setParticipants(ArrayList<Object> participants) {
        if (get_id() != null) {
            MongoCollection<Document> collection = getDb().getCollection("events");
            collection.findOneAndUpdate(new Document("_id", get_id()),
                    new Document("$set", new Document("participants", participants)));
        }
        this.participants = participants;
    }

    public void addParticipant(Object participant) {
        if (get_id() != null) {
            MongoCollection<Document> collection = getDb().getCollection("events");
            collection.findOneAndUpdate(
                    new Document("_id", get_id()),
                    new Document("$push", new Document("participants", participant)));
        }

        if (this.participants == null) {
            this.participants = new ArrayList<>();
        }
        this.participants.add(participant);
    }

    public void removeParticipant(Object participant) {
        if (get_id() != null) {
            MongoCollection<Document> collection = getDb().getCollection("events");
            collection.findOneAndUpdate(
                    new Document("_id", get_id()),
                    new Document("$pull", new Document("participants", participant)));
        }

        if (this.participants != null) {
            this.participants.remove(participant);
        }
    }

    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public long getDateTime() {
        return dateTime;
    }

    public Object getAdmin() {
        return admin;
    }

    public ArrayList<Object> getParticipants() {
        return participants;
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

        Object currentUserId = session.get("user-id");

        ArrayList<Object> eventParticipants = new ArrayList<>(Arrays.asList(currentUserId));

        Event newEvent = new Event(db, eventTitle, eventLocation, dateTimeInSeconds, currentUserId, eventParticipants);

        System.out.print("create func ");
        System.out.println(newEvent.get_id());
        sendRequest(db, currentUserId, newEvent);

        mainMenu();
    }

    public static void listUpcomingEvents(Document session, MongoDatabase db) {
        Scanner sc = new Scanner(System.in);

        Object userId = session.get("user-id");
        User user = new User(db, userId);

        ArrayList<Object> eventsList = user.getUpcomingEvents();

        int choice = -1;
        do {
            if (eventsList.isEmpty()) {
                System.out.println("You have no upcoming events :(");
                break;
            }
            System.out.println("[-2] sort by date & time ");
            System.out.println("[-1] exit");
            for (int i = 0; i < eventsList.size(); i++) {
                Object eventId = eventsList.get(i);
                Event event = new Event(db, eventId);
                String title = event.getTitle();
                String location = event.getLocation();
                long dateTimeInSeconds = event.getDateTime();
                String dateTimeString = Instant.ofEpochSecond(dateTimeInSeconds).toString();

                Object adminId = event.getAdmin();

                User admin = new User(db, adminId);

                System.out.printf("[%d] %s - %s - %s - %s\n", i, title, admin.getUsername(), dateTimeString,
                        location);

                ArrayList<Object> participants = event.getParticipants();
                System.out.print("    - Participants:");
                for (Object participantId : participants) {
                    User participant = new User(db, participantId);
                    String participantUsername = participant.getUsername();
                    System.out.print(" " + participantUsername);
                }
                System.out.println();
            }

            System.out.print("which event you would like to cancel: ");
            choice = sc.nextInt();
            if (choice < -2 || choice >= eventsList.size()) {
                System.out.println("Invalid Entry");
                continue;
            }
            if (choice == -2) {
                sortEvents(eventsList, db);
                continue;
            }

            if (choice > -1) {
                Object eventId = eventsList.get(choice);
                Event event = new Event(db, eventId);
                user.removeUpcomingEvent(eventId);
                event.removeParticipant(userId);
            } else {
                break;
            }

        } while (choice != -1);

        mainMenu();

    }

    public static void listMyEvents(Document session, MongoDatabase db) {
        Scanner sc = new Scanner(System.in);

        Object userId = session.get("user-id");
        User user = new User(db, userId);
        ArrayList<Event> events = user.getEvents(db);

        int choice = -1;
        do {
            System.out.println("Events Created By you: ");

            System.out.println("[-2] Sort By Date & Time");
            System.out.println("[-1] Exit");

            for (int i = 0; i < events.size(); i++) {
                Event event = events.get(i);
                String title = event.getTitle();
                String location = event.getLocation();
                long dateTimeInSeconds = event.getDateTime();

                String dateTimeString = Instant.ofEpochSecond(dateTimeInSeconds).toString();
                System.out.printf("[%d] %s - %s - %s\n", i, title, dateTimeString, location);

                ArrayList<Object> participants = event.getParticipants();
                System.out.print("    - Participants:");
                for (Object participantId : participants) {
                    User participant = new User(db, participantId);
                    String participantUsername = participant.getUsername();
                    System.out.print(" " + participantUsername);
                }
                System.out.println();
            }
            System.out.print("event you want to edit: ");
            choice = sc.nextInt();
            if (choice < -2 || choice >= events.size()) {
                System.out.println("Invalid Entry");
                continue;
            }

            if (choice == -2) {
                System.out.println("sort ");
                sortEvents(events);
                continue;
            }
            if (choice > -1) {
                Event selectedEvent = events.get(choice);
                String title = selectedEvent.getTitle();
                String location = selectedEvent.getLocation();
                long dateTimeInSeconds = selectedEvent.getDateTime();

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

                selectedEvent.setTitle(title);
                selectedEvent.setLocation(location);
                selectedEvent.setDateTime(dateTimeInSeconds);

                sendRequest(db, userId, selectedEvent);
            } else {
                break;
            }

        } while (choice != -1);

        mainMenu();

    }

    public static void sortEvents(ArrayList<Event> events) {
        int n = events.size();
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                if (events.get(j).getDateTime() > events.get(j + 1).getDateTime()) {
                    Event temp = events.get(j);
                    events.set(j, events.get(j + 1));
                    events.set(j + 1, temp);
                    swapped = true;
                }
            }
            if (!swapped)
                break;
        }
    }

    public static void sortEvents(ArrayList<Object> events, MongoDatabase db) {
        int n = events.size();
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                Event eventLeft = new Event(db, events.get(j));
                Event eventRight = new Event(db, events.get(j + 1));

                if (eventLeft.getDateTime() > eventRight.getDateTime()) {
                    Object eventId = events.get(j);
                    events.set(j, events.get(j + 1));
                    events.set(j + 1, eventId);
                    swapped = true;
                }
            }
            if (!swapped)
                break;
        }
    }
}
