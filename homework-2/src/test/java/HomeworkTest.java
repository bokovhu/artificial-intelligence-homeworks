import org.junit.Test;

import java.io.*;
import java.util.*;

public class HomeworkTest {

    private String generateInput () {

        final int numRatings = 60000;
        final int numUsers = 500;
        final int numBooks = 200;

        Map<Integer, Set<Integer>> alreadyRated = new HashMap<> ();

        Random random = new Random ();

        StringBuilder sb = new StringBuilder ();

        sb.append (numRatings)
                .append ("\t")
                .append (numUsers)
                .append ("\t")
                .append (numBooks)
                .append ("\n");

        for (int i = 0; i < numRatings; i++) {

            boolean generated = false;
            do {

                final int userId = random.nextInt (numUsers);
                final int bookId = random.nextInt (numBooks);

                if (!alreadyRated.containsKey (userId)
                        || !alreadyRated.get (userId).contains (bookId)) {

                    alreadyRated.computeIfAbsent (userId, k -> new HashSet<> ())
                            .add (bookId);
                    generated = true;
                    sb.append (userId)
                            .append ("\t")
                            .append (bookId)
                            .append ("\t")
                            .append (random.nextInt (5) + 1)
                            .append ("\n");

                }

            } while (!generated);

        }

        return sb.toString ();

    }

    @Test
    public void generateTestInput () throws Exception {

        try (FileWriter fw = new FileWriter ("hw2-example.in");
             BufferedWriter bw = new BufferedWriter (fw)
        ) {
            bw.write (generateInput ());
        } catch (Exception exc) {
            exc.printStackTrace ();
        }

    }

    @Test
    public void test () throws Exception {

        /*
        ByteArrayInputStream inputStream = new ByteArrayInputStream (
                generateInput ().getBytes ()
        );
        ByteArrayOutputStream outputBaos = new ByteArrayOutputStream ();
        PrintStream outputStream = new PrintStream (outputBaos);

        Main m = new Main (inputStream, outputStream);
        m.solve ();

        System.out.println (new String (outputBaos.toByteArray ()));
         */

    }

}
