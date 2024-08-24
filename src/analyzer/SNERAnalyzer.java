package analyzer;

import org.apache.lucene.analysis.Analyzer;

public class SNERAnalyzer extends Analyzer {
    public SNERAnalyzer(){
        super();
    }

    @Override
    protected TokenStreamComponents createComponents(String s) {
        TokenizerImpl tokenizer = new TokenizerImpl();
        return new TokenStreamComponents(tokenizer);
    }
}
