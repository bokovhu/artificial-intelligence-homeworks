import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class HomeworkTest {

    @Test
    public void test () throws Exception {

        String input = IOUtils.resourceToString ("/training_input.txt", StandardCharsets.US_ASCII);
        ByteArrayInputStream bais = new ByteArrayInputStream (input.getBytes ());
        ByteArrayOutputStream baos = new ByteArrayOutputStream ();
        PrintStream ps = new PrintStream (baos);

        Main m = new Main (bais, ps);
        m.solve ();

        System.out.println (
                "--- Homework output ---\n"
                + new String (baos.toByteArray ())
        );

    }

}
