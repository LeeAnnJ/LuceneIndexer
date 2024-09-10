import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoveWrapper {
    public static String removeWrapperClasses(String code) {
        String pattern = "public class (.*?)\\{(.*)\\}";
        Pattern r = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher m = r.matcher(code);
        if (m.find()) {
            return m.group(2);
        } else {
            return "NO MATCH";
        }
    }
    public static String readFileAsString(String fileName) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(fileName)));
        return content;
    }

    public static void preprocessed_Dataset() throws IOException {
        String[] datasetNames = { "StatType-SO", "Short-SO" };
        String[] libs = { "android", "gwt", "hibernate", "joda_time", "jdk", "xstream" };
        String dataset_path = "../Dataset_without_imports";
        String outputDir = "../Dataset_without_wrapperclasses";

        for (String dataset : datasetNames) {
            for (String lib : libs) {
                String lib_dir = dataset_path + "/" + dataset + "/" + lib;
                String output_lib_dir = outputDir + "/" + dataset + "/" + lib;

                // Walk through all files in the lib_dir
                Files.walk(Paths.get(lib_dir)).forEach(path -> {
                    if (Files.isRegularFile(path)) {
                        try {
                            // Read the file as a string
                            String code = readFileAsString(path.toString());

                            // Remove wrapper classes
                            code = removeWrapperClasses(code);

                            // Create the same directory structure in the output directory
                            Path outputPath = Paths.get(output_lib_dir, path.getFileName().toString());

                            // Create directories if they don't exist
                            Files.createDirectories(outputPath.getParent());

                            // Write the processed code to the new file
                            Files.write(outputPath, code.getBytes());

                            System.out.println(path.toString() + " ===> " + outputPath.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }
    public static void main(String[] args) throws Exception {
        // Step 1.5：remove wrapper classes of the dataset
        preprocessed_Dataset();
    }
}
