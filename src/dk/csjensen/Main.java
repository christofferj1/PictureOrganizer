package dk.csjensen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws IOException
    {
        try (Stream<Path> paths = Files.list(Paths.get("."))) {
            for (Path path : paths.toList()) {
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                FileTime fileTime = attributes.lastModifiedTime();
                final String date = fileTime.toString().substring(0, 10).replace("-", "");
                final String time = fileTime.toString().substring(11, 19).replace(":", "");

                final File file = path.toFile();
                final String file_extension = Arrays.stream(file.getName().split("\\.")).toList().getLast();
                final String new_name = String.format("%s_%s.%s", date,time,file_extension);

                final File destination_file = new File(new_name);
                final boolean result = file.renameTo(destination_file);
                System.out.println("File rename '" + file.getName() + "' -> '" + new_name + "': " + result);
            }
        }
    }
}
