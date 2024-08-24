package analyzer;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;


public class TokenizerImpl extends Tokenizer {
    private SNERTokenzier tokenzier;
    private final CharTermAttribute charTermAttr = addAttribute(CharTermAttribute.class);

    private int index;
    private int tlen;
    private boolean newdoc;
    private String[] tokens;

    public TokenizerImpl(){
        super();
        tokenzier = new SNERTokenzier();
        this.index = 0;
        this.tlen = 0;
        newdoc = true;
    }
    protected void readdoc() throws IOException {
        char[] buffer = new char[4096];
        StringBuilder inputText = new StringBuilder();
        int len = input.read(buffer);
        while (len>0){
            inputText.append(new String(buffer, 0, len));
            len = input.read(buffer);
        }
        try {
            tokens = tokenzier.tokenize(inputText.toString());
            tlen = tokens.length;
        } catch (SNERTokenzier.AlignmentFailed e) {
            throw new RuntimeException(e);
        }
        newdoc = false;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if(newdoc) readdoc();
        if(index<tlen){
            clearAttributes();
            charTermAttr.append(tokens[index].trim());
            index++;
            return true;
        }
        return false;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        index = 0;
        newdoc = true;
    }
}
