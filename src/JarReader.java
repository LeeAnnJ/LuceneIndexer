import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;

public class JarReader {
    public static void main(String[] args) throws IOException {
        JarFile jarFile = new JarFile(new File("./lib/json-20231013.jar"));
        jarFile.stream().forEach(entry -> {
            if(entry.getName().endsWith(".class")) {
                try {
                    InputStream inputStream = jarFile.getInputStream(entry);
                    // 在这里处理输入流，读取类文件的内容
                    // 例如，你可以使用Java反编译器将输入流内容转换为Java源代码
                    // 或者直接将输入流内容输出到文件中
                    byte[] buffer = new byte[10];
                    inputStream.read(buffer);
                    String firstTenCharacters = new String(buffer);
                    System.out.println("First ten characters of " + entry.getName() + ": " + firstTenCharacters);
                    inputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        });
    }
}
