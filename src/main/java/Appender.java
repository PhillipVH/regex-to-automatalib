import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Appender {

    private String logFile;
    private BufferedWriter writer;

    // prevent instantiation
    private Appender() {
    }

    public static Appender FileAppender(String filename) {
        Appender logger = new Appender();
        File file = new File(filename);

        // delete the previous file (if one exists)
        if (file.delete()) {
            System.out.println("Previous log deleted");
        }

        try {
            logger.writer = new BufferedWriter(new FileWriter(file, true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return logger;
    }

    public Appender line(String input) {
        try {
            writer.append(input);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    public Appender write(String input) {
        try {
            writer.append(input);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }
}
