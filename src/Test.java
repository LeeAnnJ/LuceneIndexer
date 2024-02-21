import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Test {
    public static void main(String[] args) throws Exception {
         System.out.println("Hello, World!");
         if(args.length==0)
             System.out.println("empty argument");
         else
             System.out.println("Arg0:" + args[0]);
         if(args.length>=2)
             System.out.println("Arg1:"+args[1]);
        // todo: 打包成jar包
//        Properties properties = new Properties();
//        try {
//            properties.load(new FileInputStream("../config/file_structure.ini"));
//            String database_code_folder = properties.getProperty("DATASET_CODE_FOLDER");
//            String result_original_folder = properties.getProperty("RESULT_ORIGINAL_FOLDER");
//
//            System.out.println("Java 读取配置");
//            System.out.println("database_code_folder: " + database_code_folder);
//            System.out.println("result_original_folder: " + result_original_folder);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}