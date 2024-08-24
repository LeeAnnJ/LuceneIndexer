import analyzer.SNERAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public class Test {
    public static void main(String[] args) throws Exception {
        Analyzer analyzer = new SNERAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream("field", new StringReader("I replace self.conv1() = nn.Conv2d(1, 32, 5, padding=2) with self.w_conv1 = Variable(torch.randn(1, 32, 5))"));

        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();

        while (tokenStream.incrementToken()) {
            System.out.println(charTermAttribute.toString());
        }

        tokenStream.end();
        tokenStream.close();

//         System.out.println("Hello, World!");
//         if(args.length==0)
//             System.out.println("empty argument");
//         else
//             System.out.println("Arg0:" + args[0]);
//         if(args.length>=2)
//             System.out.println("Arg1:"+args[1]);
    }
}