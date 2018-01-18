package gitlet;

import java.io.File;

/**
 * A representation of a file and its contents at a particular moment in time.
 * Created by keeley on 7/14/2017.
 */
public class Blob extends File {
    /** The name of the file. */
    private String _fileName;
    /** The unique SHA-1 ID of the file at the point it was added. */
    private String _id;
    /** The contents of the file at the point it was added. */
    private byte[] _contents;

    /** Constructs a new Blob representing the file at pathName. */
    public Blob(String pathname) {
        super(pathname);
        _fileName = pathname;
        _contents = Utils.readContents(new File(_fileName));
        _id = Utils.sha1(_contents);
    }


    /**
     * Return this blob's unique SHA-1 ID.
     */
    public String getID() {
        return _id;
    }

    /**
     * Return this blob's fileName.
     */
    public String getFileName() {
        return _fileName;
    }

    /**
     * Return this blob's contents.
     */
    public byte[] getContents() {
        return _contents;
    }
}
