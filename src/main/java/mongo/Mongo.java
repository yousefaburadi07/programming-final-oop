package mongo;

import org.bson.Document;

import com.mongodb.client.MongoDatabase;

public class Mongo {
    private Object _id;
    private MongoDatabase db;


    public Mongo(Object _id, MongoDatabase db) {
        set_id(_id);
        setDb(db);
    }

    public void set_id(Object _id) {
        this._id = _id;
    }

    public void setDb(MongoDatabase db) {
        this.db = db;
    }

    public Object get_id() {
        return _id;
    }

    public MongoDatabase getDb() {
        return db;
    }

    public void create(MongoDatabase db) {
        Document document = new Document();
        set_id(document.get("_id"));
    }
}
