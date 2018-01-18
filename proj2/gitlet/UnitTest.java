package gitlet;

import ucb.junit.textui;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** The suite of all JUnit tests for the gitlet package.
 *  @author
 */
public class UnitTest {

    /**
     * Run the JUnit tests in the loa package. Add xxxTest.class entries to
     * the arguments of runClasses to run other JUnit tests.
     */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    @Test
    public void testInit() {
        Gitlet.init();
        File gitlet = new File(".gitlet");
        File HEAD = new File(".gitlet/HEAD");
        File COMMITS = new File(".gitlet/COMMITS");
        File BLOBS = new File(".gitlet/BLOBS");
        File branches = new File(".gitlet/branches");
        File stagingArea = new File(".gitlet/staging_area");
        File marked = new File(".gitlet/marked");
        File allCommits = new File(".gitlet/allCommits");
        File allBlobs = new File(".gitlet/allBlobs");
        assertTrue(gitlet.isDirectory());
        assertTrue(COMMITS.isDirectory());
        assertTrue(BLOBS.isDirectory());
        HashMap comMap = (HashMap) Gitlet.deserialize(".gitlet/allCommits");
        assertEquals(1, comMap.size());
        String head = (String) Gitlet.deserialize(".gitlet/HEAD");
        String currComID = Gitlet.getBranchHeadCommitID(Gitlet.head());
        Commit com = (Commit) Gitlet.deserialize(".gitlet/COMMITS/" + currComID);
        assertTrue(com != null);
        assertTrue(("master").equals(head));
        return;
    }


    @Test
    public void testAddRemove() {
        //adding new file called wug, making sure file exist
        Gitlet.init();
        Blob wug1 = new Blob("testing/src/wug.txt");
        Gitlet.add("wug.txt");
        //assertTrue(Gitlet._stagingArea().containsKey(wug1.getFileName()));
       // System.out.println("Test: This should print 'File does not exist'");
       // Gitlet.add("testing/src/wug1.txt");
       // System.out.println("End test");
        //System.out.println();

        //adding unchanged files, should not be staged
        Gitlet.commit("First add");
        Gitlet.remove("wug.txt");
        assertTrue(Gitlet.marked().contains("wug.txt"));
        //Gitlet.commit("wug 1 commit");
        Gitlet.add("wug.txt");
        assertTrue(!Gitlet.stagingArea().containsKey(wug1.getFileName()));

        //modify wug1, should be staged
//        File wug = new File("testing/src/wug.txt");
//        Utils.writeContents(wug, Gitlet.serialize("testing/src/notwug.txt"));
//        Gitlet.add("testing/src/wug.txt");
//        assertTrue(Gitlet._stagingArea().containsKey(wug1.getFileName()));
//        Gitlet.commit("wug 1 changed commit");

        //removing the wug files, checking if added to marked


        //adding removed file so it should not be staged or in marked
        Gitlet.add("testing/src/wug.txt");
        assertTrue(!Gitlet.stagingArea().containsKey(wug1.getFileName()));
        assertTrue(!Gitlet.marked().contains(wug1.getFileName()));

        //removing nonexistent file
        System.out.println("Test: This should print 'No reason to remove the file.'");
        Gitlet.remove("testing/src/wug2.txt");

        //adding new file, removing, should not be in working directory, commiting, should be in marked
        Blob notwug = new Blob ("testing/src/notwug.txt");
        Gitlet.add("testing/src/notwug.txt");
        Gitlet.remove("testing/src/notwug.txt");
        assertTrue(!Gitlet.stagingArea().containsKey(notwug.getFileName()));
        assertTrue(Utils.plainFilenamesIn("testing/src").contains("wug.txt")==false);
        Gitlet.commit("wug3 commit");
        assertTrue(Gitlet.marked().contains(notwug.getFileName()));
   }

//     public void testFind() {
//         File wug1 = new File(".gitlet");//how do you add to the working directory?!
//         Gitlet.add(wug1);
//         Gitlet.commit("hello");
//         String sha = Gitlet.getCommit(Gitlet.getBranchHead(Gitlet.head())).getSnapshot().get(wug1);
//         //assertEquals(sha,Gitlet.find("hello"));
//         //assertEquals("Found no commits with that message.",gitlet.find("goodbye"));
//     }

    @Test
    public void testAddRmBranch() {
        Gitlet.init();

        Gitlet.branch("newBranch1");
        Map branches1 = Gitlet.branches();
        String head = Gitlet.head();
        assertEquals(2, branches1.size());
        assertTrue(branches1.containsKey("newBranch1"));
        assertEquals(branches1.get(head), branches1.get("newBranch1"));
        assertNotEquals("newBranch1", head);

        Gitlet.branch("newBranch1");
        Map branches2 = Gitlet.branches();
        assertEquals(2, branches2.size());

        Gitlet.rmBranch(head);
        Map branches3 = Gitlet.branches();
        assertEquals(2, branches3.size());

        Gitlet.rmBranch("notABranch");
        Map branches4 = Gitlet.branches();
        assertEquals(2, branches4.size());

        Gitlet.rmBranch("newBranch1");
        Map branches5 = Gitlet.branches();
        assertEquals(1, branches5.size());
        assertTrue(!branches5.containsKey("newBranch1"));
    }

}
