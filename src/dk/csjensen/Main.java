package dk.csjensen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main
{
  public static void main(String[] args) throws IOException
  {
    if (args.length < 1 || args.length > 2) {
      System.out.println("You must provide one argument for source directory and one optional argument (rename) if files should be renamed");
      System.exit(-1);
    }

    final File directory = new File(args[0]);
    if (!directory.exists() || !directory.isDirectory()) {
      System.out.println("First argument must be an existing directory");
      System.exit(-2);
    }

    final boolean rename = args.length == 2 && args[1].equalsIgnoreCase("rename");

    create_directories();

    moveFiles(directory, rename);
  }

  // Creates directories for each month in the directory in which the application runs.
  // If a directory (or file) with the same name exist, the operation is ignored for the given directory.
  private static void create_directories()
  {
    for (int i = 1; i <= 12; i++) {
      final String directory_name = String.format("%02d", i);
      if (!new File(directory_name).exists()) {
        boolean mkdir_result = new File(directory_name).mkdir();
        if (!mkdir_result) {
          System.out.println("Make directory:" + directory_name + " failed");
          System.exit(-2);
        }
      }
    }
  }

  private static void moveFiles(final File directory, final boolean rename) throws IOException
  {
    final List<String> fileNamesUsed = new ArrayList<>();
    try (Stream<Path> paths = Files.list(directory.toPath())) {
      for (Path path : paths.toList()) {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        final FileTime fileTime = attributes.lastModifiedTime();
        final String month = fileTime.toString().substring(5, 7); // String format is yyyy-MM-dd...

        final String targetFileName = rename && fileNameIsNotDateTime(path.getFileName().toString()) ? dateFileName(path, fileTime.toString(), fileNamesUsed) : path.getFileName().toString();

        fileNamesUsed.add(targetFileName);

        final Path target = Path.of(month, targetFileName);

        Files.move(path, target);
      }
    }
  }

  private static boolean fileNameIsNotDateTime(final String fileName)
  {
    // Checks if the file name is NOT dddddddd_dddddd.<ext> ('d' means digit)
    // It should only rename files that aren't already in a date time format
    return !Pattern.matches("\\d{8}_\\d{6}\\..+", fileName);
  }

  private static String dateFileName(final Path path, final String fileTime, final List<String> fileNamesUsed)
  {
    final String date = fileTime.substring(0, 10).replace("-", "");
    final String time = fileTime.substring(11, 19).replace(":", "");

    final String file_extension = Arrays.stream(path.getFileName().toString().split("\\.")).toList().getLast();
    String fileName = String.format("%s_%s.%s", date, time, file_extension);

    fileName = handleFileNameCollisions(fileNamesUsed, fileName);

    return fileName;
  }

  private static String handleFileNameCollisions(List<String> fileNamesUsed, String fileName)
  {
    // Check if the file name exists on the target, and if so, add a number like this:
    // 20240206_123456.jpg -> 20240206_123456_1.jpg (increment the number when multiple duplicates exists)
    while (fileNamesUsed.contains(fileName)) {
      final String[] fileNameParts = fileName.split("[_\\.]"); // Split on both _ and .
      if (fileNameParts.length == 3) // Add number one to duplicate
        fileName = String.format("%s_%s_1.%s", fileNameParts[0], fileNameParts[1], fileNameParts[2]);
      else if (fileNameParts.length == 4) // Increment the number to handle multiple duplicates
        fileName = String.format("%s_%s_%d.%s", fileNameParts[0], fileNameParts[1], Integer.parseInt(fileNameParts[2]) + 1, fileNameParts[3]);
      else throw new RuntimeException("Date-time file name has weird format: " + fileName);
    }
    return fileName;
  }
}
