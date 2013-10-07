import com.jjencoder.JJencoder;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class MainTest extends TestCase {
    private JJencoder encoder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        encoder = new JJencoder();
    }

    public void testDecode() throws IOException {
        String src = FileUtils.readFileToString(new File("test_data/test1.txt"));
        assertEquals("ENCODED", encoder.decode(src));
        encoder.reset();

        src = FileUtils.readFileToString(new File("test_data/test3.txt"));
        String result = FileUtils.readFileToString(new File("test_data/test3_result.txt"));
        assertEquals(result, encoder.decode(src));
        encoder.reset();

        src = FileUtils.readFileToString(new File("test_data/test2.txt"));
        assertEquals("localStorage.setItem(\"gv-token\", \"5f05411643807843b30ad3f033430b47\");", encoder.decode(src));
        encoder.reset();

        src = FileUtils.readFileToString(new File("test_data/test4.txt"));
        result = FileUtils.readFileToString(new File("test_data/test4_result.txt"));
        assertEquals(result, encoder.decode(src));
    }

}
