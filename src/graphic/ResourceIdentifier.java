package graphic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceIdentifier {
    public static void main(String[] args) {
        String directory = "src\\graphic\\resources\\models";
        try {
            Path dir = Paths.get(directory);
            Files.walk(dir).forEach(path -> {
                importFile(path.toFile());
            });
        } catch (Exception e) {
            System.out.println("Error importing assets!");
        }
    }


    private static void importFile(File file){
        if (file.isDirectory()) {
            System.out.println("Directory: " + file.getAbsolutePath());
        } else {
            System.out.println("File: " + file.getAbsolutePath());
            String extension = "";
            int i = file.getName().lastIndexOf('.');
            if (i > 0) {
                extension = file.getName().substring(i+1);
            }
            System.out.println(extension);
        }
    }
}
