package dk.csjensen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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

    System.out.printf("Organizing pictures from %s%s\n", directory, rename ? " while RENAMING" : "");

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
    try (Stream<Path> paths = Files.list(directory.toPath())) {
      for (Path path : paths.toList()) {
        final String fileTime = Files.readAttributes(path, BasicFileAttributes.class).lastModifiedTime().toString();

        String targetFileName = path.getFileName().toString();
        if (rename)
          targetFileName = dateFileName(targetFileName, fileTime);

        final String month = fileTime.substring(5, 7); // String format is yyyy-MM-dd...
        targetFileName = handleFileNameCollisions(targetFileName, month);

        final Path target = Path.of(month, targetFileName);
        try {
          Files.move(path, target);
        } catch (IOException e) {
          System.out.println("Unable to move " + path.getFileName() + " to " + target);
          throw e;
        }
      }
    }
  }

  private static String dateFileName(final String fileName, final String fileTime)
  {
    final String date = fileTime.substring(0, 10).replace("-", "");
    final String time = fileTime.substring(11, 19).replace(":", "");

    final String file_extension = Arrays.stream(fileName.split("\\.")).toList().getLast();
    return String.format("%s_%s.%s", date, time, file_extension);
  }

  private static String handleFileNameCollisions(final String fileName, final String directoryName)
  {
    final int tries = 10;
    String result = fileName;

    if (fileNamesUsedInTargetLocation(directoryName).contains(result))
      result = appendFileNamePostfix(result);

    for (int i = 0; i < tries - 1; i++) {
      if (fileNamesUsedInTargetLocation(directoryName).contains(result))
        result = incrementFileNamePostfix(result);
      else {
        if (!fileName.equals(result))
          System.out.println("File name: " + fileName + " became " + result);
        return result;
      }
    }
    throw new RuntimeException(
      String.format("Unable to find name for %s in directory %s in %d tries", fileName, directoryName, tries));
  }

  private static List<String> fileNamesUsedInTargetLocation(final String targetDirectoryName)
  {
    final File targetDirectory = new File(targetDirectoryName);
    if (!targetDirectory.exists() || !targetDirectory.isDirectory())
      throw new RuntimeException("Target directory '" + targetDirectoryName + "' does not exists");
    try (Stream<Path> stream = Files.list(targetDirectory.toPath())) {
      return stream.toList().stream().map(p -> p.getFileName().toString()).collect(Collectors.toList());
    } catch (final IOException e) {
      throw new RuntimeException("Unable to list files in target directory: " + targetDirectoryName, e);
    }
  }

  private static String appendFileNamePostfix(final String fileName)
  {
    // The regular expression tells Java to split on any period that is followed by any number of non-periods, followed
    // by the end of input. There is only one period that matches this definition (namely, the last period).
    final String[] fileNameParts = splitFileNameBaseAndExtension(fileName);
    return fileNameParts[0] + "_1." + fileNameParts[1];
  }

  private static String incrementFileNamePostfix(final String fileName)
  {
    final String[] fileNameParts = splitFileNameBaseAndExtension(fileName);
    final String[] fileNameBaseParts = fileNameParts[0].split("_");
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < fileNameBaseParts.length - 1; i++) {
      result.append(fileNameBaseParts[i]).append("_");
    }
    result.append(Integer.parseInt(fileNameBaseParts[fileNameBaseParts.length - 1]) + 1);
    result.append(".").append(fileNameParts[1]);
    return result.toString();
  }

  private static String[] splitFileNameBaseAndExtension(String fileName)
  {
    final String[] fileNameParts = fileName.split("\\.(?=[^\\.]+$)");
    if (fileNameParts.length != 2)
      throw new RuntimeException("Unable to split " + fileName + " into base and extension");
    return fileNameParts;
  }
}
