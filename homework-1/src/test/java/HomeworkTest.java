import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringBufferInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HomeworkTest {

    @Test
    public void testUsingExampleInput () throws Exception {

        String input = IOUtils.resourceToString ("/example.in", StandardCharsets.UTF_8);
        String expectedOutput = IOUtils.resourceToString ("/example.out", StandardCharsets.UTF_8);

        ByteArrayOutputStream programOutputStream = new ByteArrayOutputStream ();
        Main program = new Main (
                new ByteArrayInputStream (input.getBytes ()),
                new PrintStream (programOutputStream)
        );
        program.solve ();

        String producedOutput = new String (programOutputStream.toByteArray ());

        System.out.println (producedOutput);

    }

}
