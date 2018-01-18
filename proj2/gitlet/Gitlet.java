package gitlet;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by keeley on 7/12/2017.
 */
public class Gitlet {

    /** Returns the name of the current branch from a serializable file. */
    public static String head() {
        return (String) deserialize(".gitlet/HEAD");
    }

    /** Returns a Map of all initialized branches from a serializable file. Keys are branch names,
     * values are the unique SHA-1 IDs of the Commit at the head of each branch. */
    public static Map branches() {
        return (Map) deserialize(".gitlet/branches");
    }

    /** Returns a Map representing the current staging area containing files added but not
     * committed from a Serializable file.
     * Keys are file names, values are BLOB objects containing the
     * contents of the file with filename at the time of being added to the staging area.*/
    public static Map stagingArea() {
        return (HashMap<String, Blob>) deserialize(".gitlet/staging_area");
    }

    /** Returns a list of currently tracked files marked for removal from a Serializable file. */
    public static List marked() {
        return (ArrayList<String>) deserialize(".gitlet/marked");
    }

    public static void init() throws GitletException { //constant time
        File gitlet = (new File(".gitlet"));
        if (gitlet.exists()) {
            throw new GitletException("A gitlet version-control system "
                    + "already exists in the current directory.");
        }
        gitlet.mkdir();
        File commits = new File(gitlet, "COMMITS");
        File head = new File(gitlet, "HEAD");
        File branches = new File(gitlet, "branches");
        File stagingArea = new File(gitlet, "staging_area");
        File marked = new File(gitlet, "marked");
        File blobs = new File(gitlet, "BLOBS");
        commits.mkdir();
        blobs.mkdir();
        Commit com  = new Commit("initial commit");
        HashMap branchesMap = new HashMap<String, String>();
        branchesMap.put("master", com.getID());
        Utils.writeContents(branches, serialize(branchesMap));
        Utils.writeContents(stagingArea, serialize(new HashMap<String, Blob>()));
        Utils.writeContents(marked, serialize(new ArrayList<String>()));
        addCommit(com);
        setHead("master");
    }

    /**
     * Serializes an object OBJ (i.e. converts the object to a
     * byte[]) and returns the byte array representation of OBJ.
     */
    public static byte[] serialize(Object obj) {
        // Taken directly from spec.
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(obj);
            objectStream.close();
            return stream.toByteArray();
        } catch (IOException excp) {
            System.out.println("Internal error serializing object.");
            return null;
        }
    }

    /** Deserializes the file at PATH (i.e. converts the file's contents
     * to an object) and returns the object. */
    public static Object deserialize(String path) {
        //Taken almost directly from spec.
        File inFile = new File(path);
        try {
            ObjectInputStream inp =
                    new ObjectInputStream(new FileInputStream(inFile));
            return inp.readObject();
        } catch (IOException | ClassNotFoundException excp) {
            return null;
        }
    }


    public static void add(String fileName) throws GitletException {
        File curr = new File(fileName);
        Map<String, Blob> stagingArea = stagingArea();
        List<String> marked = marked();
        if (!curr.exists()) {
            throw new GitletException("File does not exist.");
        } else {
            Blob newBlob = new Blob(fileName);
            Map<String, String> snap = getCurrentCommit().getSnapshot();
            if (snap.size() == 0) {
                stagingArea.put(newBlob.getFileName(), newBlob);
            }
            for (Map.Entry<String, String> entry : snap.entrySet()) {
                String file = entry.getKey();
                String id = entry.getValue();
                if (marked.contains(newBlob.getFileName())) {
                    marked.remove(newBlob.getFileName());
                } else if (!(snap.containsKey(newBlob.getFileName()))) {
                    stagingArea.put(newBlob.getFileName(), newBlob);
                } else if (newBlob.getFileName().equals(file) && newBlob.getID().equals(id)) {
                    return;
                } else if (newBlob.getName().equals(file) && !newBlob.getID().equals(id)) {
                    stagingArea.put(newBlob.getFileName(), newBlob);
                }
            }
        }

        Utils.writeContents(new File(".gitlet/staging_area"), serialize(stagingArea));
        Utils.writeContents(new File(".gitlet/marked"), serialize(marked));

    }


    public static void remove(String fileName) throws GitletException { //theta(1)
        Map<String, String> snap = getCurrentCommit().getSnapshot();
        Map<String, Blob> stagingArea = stagingArea();
        List<String> marked = marked();

        if (!snap.containsKey(fileName) && !stagingArea.containsKey(fileName)) {
            throw new GitletException("No reason to remove the file.");

        } else if (snap.containsKey(fileName) && stagingArea.containsKey(fileName)) {
            marked.add(fileName);
            stagingArea.remove(fileName);
            Utils.restrictedDelete(fileName);

        } else if (stagingArea.containsKey(fileName)) {
            stagingArea.remove(fileName);


        } else if (snap.containsKey(fileName)) {
            marked.add(fileName);
            Utils.restrictedDelete(fileName);

        }


        Utils.writeContents(new File(".gitlet/staging_area"), serialize(stagingArea));
        Utils.writeContents(new File(".gitlet/marked"), serialize(marked));


    }

    /** Prints the ID, timestamp, and message of each commit from the current head
     * to the initial commit in order. */
    public static void log() {
        String currID = getBranchHeadCommitID(head());
        while (!currID.equals("0")) {
            Commit c = getCommit(currID);
            System.out.println("===");
            System.out.println("Commit " + currID);
            System.out.println(c.getTime());
            System.out.println(c.getLog());
            System.out.println();
            currID = c.getParentID();
        }
    }

    public static void globalLog() {
        String[] commits = (new File(".gitlet/COMMITS")).list();
        for (String id : commits) {
            Commit c = getCommit(id);
            System.out.println("===");
            System.out.println("Commit " + id);
            System.out.println(c.getTime());
            System.out.println(c.getLog());
            System.out.println();
        }
    }

    public static void find(String commitMessage) throws GitletException {
        Boolean exist = false;
        String[] commits = (new File(".gitlet/COMMITS")).list();
        for (String id : commits) {
            if (getCommit(id).getLog().equals(commitMessage)) {
                System.out.println(id);
                exist = true;
            }
        }
        if (!exist) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    public static void status() {
        System.out.println("=== Branches ===");
        branches().keySet().stream().sorted().forEach((branch) -> {
            if (branch.equals(head())) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        });
        System.out.println();

        System.out.println("=== Staged Files ===");
        stagingArea().keySet().stream().sorted().forEach((stagedFile) ->
                System.out.println(stagedFile));
        System.out.println();

        System.out.println("=== Removed Files ===");
        marked().stream().sorted().forEach((toRemove) ->
                System.out.println(toRemove));
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();

    }

    /** Puts the version of FILENAME from the head Commit in the working directory.
     *  May overwrite the version of FILENAME currently in the working directory.
     * @param fileName : the SHA-1 ID of the desired file.
     */
    public static void checkoutFile(String fileName) {
        String commitID = (String) branches().get(head());
        checkoutFileFromCommit(commitID, fileName);
    }

    public static void checkoutFileFromCommit(String commitID,
                                              String fileName) throws GitletException {
        Commit c = getCommit(commitID);
        if (c == null) {
            throw new GitletException("No commit with that id exists.");
        }
        String blobName = c.getSnapshot().get(fileName);
        if (blobName == null) {
            throw new GitletException("File does not exist in that commit.");
        }
        File checkedOut = new File(fileName);
        Blob b = getBlob(blobName);
        Utils.writeContents(checkedOut, b.getContents());
    }

    public static void checkoutBranch(String branchName) throws GitletException {
        Map branches = branches();
        if (!branches.containsKey(branchName)) {
            throw new GitletException("No such branch exists.");
        } else if (branchName.equals(head())) {
            throw new GitletException("No need to checkout the current branch.");
        } else {
            String headCommitID = (String) branches.get(branchName);
            checkoutHelper(getCommit(headCommitID));
            setHead(branchName);
        }
        //theta(N) with respect to total size of files in commit snapshot
        //theta(1) with respect to number of branches
        //theta(1) with respect to number of commits
    }

    /** Create a new branch pointed at the current head node.
     * If a branch named branchName already exists, exits.
     * @param branchName: the name of the new branch.
     */
    public static void branch(String branchName) throws GitletException {
        Map branches = branches();
        if (branches.containsKey(branchName)) {
            throw new GitletException("A branch with that name already exists.");
        } else {
            branches.put(branchName, getBranchHeadCommitID(head()));
            Utils.writeContents(new File(".gitlet/branches"), serialize(branches));
        }
    }

    /** Delete the branch BRANCHNAME. If no such branch exists, exits. */
    public static void rmBranch(String branchName) throws GitletException {
        Map branches = branches();
        if (!branches.containsKey(branchName)) {
            throw new GitletException("A branch with that name does not exist.");
        } else if (branchName.equals(head())) {
            throw new GitletException("Cannot remove the current branch.");
        } else {
            branches.remove(branchName);
            Utils.writeContents(new File(".gitlet/branches"), serialize(branches));
        }
    }

    public static void reset(String commitID) throws GitletException {
        Commit c = getCommit(commitID);
        if (c == null) {
            throw new GitletException("No commit with that id exists.");
        }
        checkoutHelper(c);
        Map updatedBranches = branches();
        updatedBranches.put(head(), commitID);
        Utils.writeContents(new File(".gitlet/branches"), serialize(updatedBranches));

    }
    public static void checkoutHelper(Commit c) throws GitletException {
        Map currSnapshot = getCurrentCommit().getSnapshot();
        Set currSnapFiles = currSnapshot.keySet();
        Map resetSnapshot = c.getSnapshot();
        Set resetSnapFiles = resetSnapshot.keySet();
        checkUntrackedFiles(resetSnapshot);
        for (Object fileName : resetSnapFiles) {
            File checkedOut = new File((String) fileName);
            Blob b = getBlob((String) resetSnapshot.get(fileName));
            Utils.writeContents(checkedOut, b.getContents());
        }
        currSnapFiles.removeAll(resetSnapFiles);
        for (Object file : currSnapFiles) {
            Utils.restrictedDelete((String) file);
        }
        Map stagingArea = stagingArea();
        stagingArea.clear();
        Utils.writeContents(new File(".gitlet/staging_area"), serialize(stagingArea));
    }


    public static void merge(String otherBranch) throws GitletException {
        if (stagingArea().size() != 0 || marked().size() != 0) {
            throw new GitletException("You have uncommitted "
                    + "changes.");
        }
        if (!branches().keySet().contains(otherBranch)) { //Check if branchName exists
            throw new GitletException("A branch with that name does not exist.");
        }
        if (otherBranch.equals(head())) { //Check if branchName is current branch
            throw new GitletException("Cannot merge a branch with itself.");
        }
        Commit currCommit = getCurrentCommit();
        Map currSnap = currCommit.getSnapshot();
        Commit otherCommit = getCommit(getBranchHeadCommitID(otherBranch));
        Map otherSnap = otherCommit.getSnapshot();
        Commit splitPoint = getSplitPoint(currCommit.getID(), otherCommit.getID());
        Map splitPointSnap = splitPoint.getSnapshot();

        checkUntrackedFiles(otherSnap);
        if (otherCommit.getID().equals(splitPoint.getID())) {
            System.out.print("Given branch is an ancestor of the current branch.");
            return;
        }
        if (currCommit.getID().equals(splitPoint.getID())) {
            setHead(otherBranch);
            System.out.print("Current branch fast-forwarded.");
            return;
        }
        Map superSnap = new HashMap(currSnap);
        superSnap.putAll(otherSnap);
        superSnap.putAll(splitPointSnap);
        Set allTrackedFiles = superSnap.keySet();
        Map stagingArea = stagingArea();
        boolean mergeConflict = false;
        for (Object file : allTrackedFiles) {
            String currSHA = (String) currSnap.get(file);
            String otherSHA = (String) otherSnap.get(file);
            String splitPointSHA = (String) splitPointSnap.get(file);

            if (isSameContent(currSHA, splitPointSHA) && !isSameContent(currSHA, otherSHA)) {
                if (otherSHA == null) {
                    remove((String) file);
                } else {
                    checkoutFileFromCommit(otherCommit.getID(), (String) file);
                    stagingArea.put(file, getBlob(otherSHA));
                }
            } else if (!isSameContent(currSHA, splitPointSHA) && !isSameContent(currSHA, otherSHA)
                    && !isSameContent(otherSHA, splitPointSHA)) {
                mergeConflict = true;
                File currFile = new File((String) file);
                String headStr = "<<<<<<< HEAD\n";
                String currContents = (currSHA != null)
                        ? new String(getBlob(currSHA).getContents(), StandardCharsets.UTF_8)
                        : "";
                String sepStr = "=======\n";
                String otherContents = (otherSHA != null)
                        ? new String(getBlob(otherSHA).getContents(), StandardCharsets.UTF_8)
                        : "";
                String endStr = ">>>>>>>\n";
                String allContents = headStr + currContents + sepStr + otherContents + endStr;

                Utils.writeContents(currFile, allContents.getBytes(StandardCharsets.UTF_8));
            }
        }
        Utils.writeContents(new File(".gitlet/staging_area"), serialize(stagingArea));
        if (mergeConflict) {
            System.out.println("Encountered a merge conflict.");
        } else {
            String message = "Merged " + head() + " with " + otherBranch + ".";
            commit(message);
        }
    }

    /** Compares two Strings A and B (one of both of which may be null).
     * Returns true iff A and B are both null or A and B both reference
     * identical Strings.
     */
    public static boolean isSameContent(String a, String b) {
        if (a == null) {
            return a == b;
        } else {
            return a.equals(b);
        }
    }

    /** Returns the Commit at the split point of two Commits with IDs A and B
     * (i.e. the most recent common ancestor of A and B).

     */
    public static Commit getSplitPoint(String current, String notCurrent) throws GitletException {
        HashSet<String> seen = new HashSet<String>();
        while (!current.equals("0")) {
            if (!seen.add(current)) {
                return getCommit(current);
            }
            current = getCommit(current).getParentID();
            if (!notCurrent.equals("0")) {
                String temp = current;
                current = notCurrent;
                notCurrent = temp;
            }
        }
        throw new GitletException("Reached initial commit without finding split point.");
    }

    /** Checks for the presence of untracked files in the current commit.
     * Throws a GitletException if untracked files found.
     */
    public static void checkUntrackedFiles(Map otherSnapshot) throws GitletException {
        Map currSnapshot = getCurrentCommit().getSnapshot();
        Set currSnapFiles = (new HashMap(currSnapshot)).keySet();
        List workingDir = Utils.plainFilenamesIn(System.getProperty("user.dir"));
        Set stagingAreaFiles = (new HashMap(stagingArea())).keySet();
        Set otherSnapFiles = (new HashMap(otherSnapshot)).keySet();
        for (Object f:otherSnapFiles) {
            if (workingDir.contains(f) && !currSnapFiles.contains(f)) {
                throw new GitletException("There is an untracked file in the way; "
                        + "delete it or add it first.");
            }
        }
    }

    // GETTERS AND SETTERS


    public static void commit(String message) throws GitletException {
        if (message.equals("")) {
            throw new GitletException("Please enter a commit message.");

        } else if (stagingArea().isEmpty() && marked().isEmpty()) {
            throw new GitletException("No changes added to the commit.");
        }
        Map<String, Blob> stagingArea = stagingArea();
        Commit newCommit = new Commit(getCurrentCommit().getID(), message);
        for (String filename: stagingArea.keySet()) {
            Blob fileBlob = stagingArea.get(filename);
            Utils.writeContents(new File(".gitlet/BLOBS/" + fileBlob.getID()), serialize(fileBlob));
            newCommit.addToSnapshot(filename, fileBlob.getID());
        }
        List<String> marked = marked();
        for (String toRemove: marked) {
            newCommit.removeFromSnapshot(toRemove);
        }
        stagingArea.clear();
        marked.clear();
        Utils.writeContents(new File(".gitlet/staging_area"), serialize(stagingArea));
        Utils.writeContents(new File(".gitlet/marked"), serialize(marked));
        addCommit(newCommit);
        setBranch(newCommit.getID());
    }

    /** Takes in a COMMITID and returns the corresponding Commit object. */
    public static Commit getCommit(String commitID) {
        if (commitID.length() < 40) {
            String[] allCommits = (new File(".gitlet/COMMITS")).list();
            for (String commit : allCommits) {
                if (commit.startsWith(commitID)) {
                    return (Commit) deserialize(".gitlet/COMMITS/" + commit);
                }
            }
            return null;
        }
        return (Commit) deserialize(".gitlet/COMMITS/" + commitID);
    }

    public static void addCommit(Commit com) {
        File f = new File(".gitlet/COMMITS/" + com.getID());
        Utils.writeContents(f, serialize(com));
    }

    /** Returns the commit ID of the Commit at the head of branch BRANCH_NAME. */
    public static String getBranchHeadCommitID(String branchName) {
        Map branches = branches();
        return (String) branches.get(branchName);
    }

    /**
     * Sets current branch to
     * @param comID representing new current commit.
     */
    public static void setBranch(String comID) {
        Map<String, String> branches = branches();
        branches.replace(head(), comID);
        Utils.writeContents(new File(".gitlet/branches"), serialize(branches));
    }

    /**
     * Returns the HEAD commit.
     */
    public static Commit getCurrentCommit() {
        return getCommit(getBranchHeadCommitID(head()));
    }

    /**
     *  Takes in a String BRANCH (a branch name) and sets HEAD
     *  to that branch name. (i.e. rewrites the contents
     *  of .gitlet/HEAD to be a new string.)
     */
    public static void setHead(String branch) {
        Utils.writeContents(new File(".gitlet/HEAD"), serialize(branch));
    }

    /** Given a blob's name blobName, returns the corresponding Blob object.
     *  Returns null if no Blob exists with that name.
     */
    public static Blob getBlob(String blobName) {
        String path = ".gitlet/BLOBS/" + blobName;
        return (Blob) deserialize(path);
    }
}
