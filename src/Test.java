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
    }
}