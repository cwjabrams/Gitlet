package gitlet;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

/**
 * Created by Abrams on 7/12/17.
 */
public class Commit implements Serializable {
    /** The unique SHA-1 label of this Commit. */
    private String _ID;
    /** The timestamp of this commit. */
    private String _time;
    /** The commit's log message. */
    private String _log;
    /** The unique SHA-1 label of this Commit's parent. */
    private String _parentID;
    /** A list of currently tracked files. Keys are file names, values are SHA-1*/
    private Map<String, String> _snapshot;

    /** Initializes a new Commit. */
    public Commit(String parentID, String msg) {
        _parentID = parentID;
        _log = msg;
        //need to copy unchanged tracked blob key-val pairs from parent, add files in staging area,
        //remove files tagged for removeal.
        _snapshot = Gitlet.getCommit(parentID).getSnapshot();
        _time = ZonedDateTime.now().toString();
        _time = _time.substring(0, 10) + " " + _time.substring(11, 19);
        ArrayList identifyingObjects = new ArrayList();
        identifyingObjects.add(_parentID);
        identifyingObjects.add(_snapshot.toString());
        identifyingObjects.add(_log);
        identifyingObjects.add(_time);
        /**
         * They did say we might want to add an extra parameter to sha-1 keys to help
         * us identify between different types of blob. Not sure how to implement that
         * yet but figure it will come up later as we work through.
         */
        _ID = Utils.sha1(identifyingObjects);
    }

    /**
     * Initialized a new commit with only a log message. Only
     * ever used for our initial commit.
     * @param msg
     */
    public Commit(String msg) {
        _parentID = "0";
        _log = msg;
        _snapshot = new HashMap<String, String>();
        _time = ZonedDateTime.now().toString();
        _time = _time.substring(0, 10) + " " + _time.substring(11, 19);
        ArrayList identifyingObjects = new ArrayList();
        identifyingObjects.add(_parentID);
        identifyingObjects.add(_snapshot.toString());
        identifyingObjects.add(_log);
        identifyingObjects.add(_time);
        /**
         * They did say we might want to add an extra parameter to sha-1 keys to help
         * us identify between different types of blob. Not sure how to implement that
         * yet but figure it will come up later as we work through.
         */
        _ID = Utils.sha1(identifyingObjects);
    }


    /**
     * Returns this commit's snapshot.
     */
    public Map<String, String> getSnapshot() {
        return new HashMap<String, String>(_snapshot);
    }

    /**
     * Gives this commit's snapshot the key FILENAME and
     * with value BLOBID.
     */
    public void addToSnapshot(String filename, String blobID) {
        if (_snapshot.containsKey(filename)) {
            _snapshot.replace(filename, blobID);
        } else {
            _snapshot.put(filename, blobID);
        }
    }

    /**
     * Removes given file from this commit's snapshot.
     */
    public void removeFromSnapshot(String filename) {
        if (_snapshot.keySet().contains(filename)) {
            _snapshot.remove(filename);
        }
    }

    /**
     * Return this commit's unique SHA-1 ID.
     */
    public String getID() {
        return _ID;
    }

    /** Return this commit's log message. */
    public String getLog() {
        return _log;
    }

    /** Return this commit's timestamp. */
    public String getTime() {
        return _time;
    }

    /**
     * Returns this commit's parent commit's unique SHA-1 ID.
     */
    public String getParentID() {
        return _parentID;
    }


}
