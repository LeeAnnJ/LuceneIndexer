package analyzer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SNERTokenzier {
    public SNERTokenzier(){;}

    public static Pattern myCompile(String pat) {
        return Pattern.compile(pat);
    }

    protected static String regexOr(String... items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            sb.append(items[i]);
            if (i < items.length - 1)
                sb.append("|");
        }
        return "(" + sb.toString() + ")";
    }
    protected static String posLookahead(String r) {
        return "(?=" + r + ")";
    }
    protected static String optional(String r){
        return "(" + r + ")?";
    }

    private static String NormalEyes = "[:=8]";
    private static String Wink = "[;]";
    private static String NoseArea = "(|o|O|-)";  // rather tight precision, \S might be reasonable...
    private static String HappyMouths = "[D\\)\\]]";
    private static String SadMouths = "[\\[\\(]";
    private static String Tongue = "[pPb]";
    private static String OtherMouths = "[\\|doO/\\\\]"; // remove forward slash if http://'s aren't cleaned
    private static String Faces = (
        "(" +
        "("+NormalEyes+"|"+Wink+")" +
        NoseArea +
        "("+Tongue+"|"+OtherMouths+"|"+SadMouths+"+|"+HappyMouths+"+)" +
        "|" +
        "("+SadMouths+"+|"+HappyMouths+"+)" +
        NoseArea +
        "("+NormalEyes+"|"+Wink+")" +
        ")"
    );
    static String Hearts = "(<+/?3+)";

    static String Arrows = "(<*[-=]*>+|<+[-=]*>*)";
    static String Emoticon = "("+Hearts+"|"+Faces+"|"+Arrows+")";
    static String PunctChars = "['“\".?!,(/:;]";
    static String PunctSeq = "['`\\\"“”‘’)]+|[.?!,…]+|[:;/(]+";
    static String Entity = "&(amp|lt|gt|quot);";
    static String UrlStart1 = regexOr("https?://", "www\\.");
    static String CommonTLDs = regexOr("com","co\\.uk","org","net","info","ca","edu","gov");
    static String UrlStart2 = "[a-z0-9\\.-]+?" + "\\." + CommonTLDs + posLookahead("[/ \\W\\\\b]");
    static String UrlBody = "[^ \\t\\r\\n<>]*?"; // * not + for case of:  "go to bla.com." -- don't want period
    static String UrlExtraCrapBeforeEnd = regexOr(PunctChars, Entity) + "+?";
    static String UrlEnd = regexOr( "\\.\\.+", "[<>]", "\\s", "$"); // added by Deheng
    static String Url = ("\\b" +
        regexOr(UrlStart1, UrlStart2) +
        UrlBody +
        posLookahead(optional(UrlExtraCrapBeforeEnd) + UrlEnd));
    static String EmailName = "([a-zA-Z0-9-_+]+[.])*[a-zA-Z0-9-_+]+";
    static String EmailDomain = "([a-zA-Z0-9]+[.])+" + CommonTLDs;
    static String Email = EmailName + "@" + EmailDomain;
    static String Hashtag = "#[a-zA-Z0-9_]+";
    static String AtMention = "@[a-zA-Z0-9_]+";
    static String Timelike = "\\d+:\\d+";
    static String NumNum = "\\d+\\.\\d+";
    static String NumberWithCommas = "(\\d+,)+?\\d{3}" + posLookahead(regexOr("[^,]","$"));
    static String BoundaryNotDot = regexOr("\\s", "[“\"?!,:;]", Entity);
    static String aa1 = "([A-Za-z]\\.){2,}" + posLookahead(BoundaryNotDot);
    static String aa2 = "([A-Za-z]\\.){1,}[A-Za-z]" + posLookahead(BoundaryNotDot);
    static String ArbitraryAbbrev = regexOr(aa1,aa2);
    static String Separators = regexOr("--+", "―");
    static String Decorations = " [  ♫   ]+ ".replace(" ","");
    static String EmbeddedApostrophe = "\\S+'\\w+";  // \S -> \w, by Deheng
    static String API = regexOr("((\\w+)\\.)+\\w+\\(\\)", "\\w+\\(\\)", "((\\w+)\\.)+(\\w+)");
    static String plural = "\\w+\\(s\\)";
    static String ProgrammingOperators = regexOr("==","!=",">=","<=","&&", "\\|\\|");
    private static String[] ProtectThese = {
        Emoticon,
        Url,
        Email,
        Entity,
        Hashtag,
        AtMention,
        Timelike,
        NumNum,
        NumberWithCommas,
        ArbitraryAbbrev,
        Separators,
        Decorations,
        EmbeddedApostrophe,
        API,  // Deheng
        PunctSeq,  // Deheng
        plural,   // Deheng
        ProgrammingOperators  // Deheng
    };
    // fun: copy and paste outta http://en.wikipedia.org/wiki/Smart_quotes
    // Removed < and > because they were causing problems with <3 emoticons
    private static String EdgePunct = "[  ' \" “ ” ‘ ’ * « » { } ( ) [ \\\\]  ]".replace(" ","");
    private static String NotEdgePunct = "[0-9A-Za-z]";
    private static String EdgePunctLeft = String.format("(\\s|^)(%s+)(%s)", EdgePunct, NotEdgePunct);
    private static String EdgePunctRight = String.format("(%s(?:\\(s?\\))?)(%s*)(\\s|$)", NotEdgePunct, EdgePunct);

    private static Pattern EdgePunctLeft_RE = myCompile(EdgePunctLeft);
    private static Pattern EdgePunctRight_RE= myCompile(EdgePunctRight);
    private static final Pattern WS_RE = myCompile("\\s+");
    private static Pattern Protect_RE = myCompile(regexOr(ProtectThese));

    public static String squeezeWhitespace(String s) {
        String newString = WS_RE.matcher(s).replaceAll(" ");
        return newString.trim();
    }
    public static class AlignmentFailed extends Exception {
        public AlignmentFailed(String message) {
            super(message);
        }
    }
    public static class Tokenization {
        //    list of tokens, plus extra info
        private List<String> tokens = new ArrayList<String>();
        public List<Integer> alignments = new ArrayList<Integer>();
        public String text = "";

        public Tokenization() {}

        public List<String> getTokens() {
            return tokens;
        }
        public void add(String token){
            tokens.add(token);
            return;
        }
        public void addAll(List<String> t){
            tokens.addAll(t);
        }
    }

    protected static String edgePunctMerge(String s){
        Matcher matcherLeft = EdgePunctLeft_RE.matcher(s);
        s = matcherLeft.replaceAll("$1$2 $3");
        Matcher matcherRight = EdgePunctRight_RE.matcher(s);
        s = matcherRight.replaceAll("$1 $2$3");
        return s;
    }
    protected static List<String> unprotectedTokenize(String s) {
        return Arrays.asList(s.split("\\s+"));
    }
    public static List<String> simpleTokenize(String text){
        String s = text;
        s = edgePunctMerge(s);
//      strict alternating ordering through the string. first and last are goods.
        List<String> goods = new ArrayList<>();
        List<String> bads = new ArrayList<>();
        int i = 0;

        Matcher matcher = Protect_RE.matcher(s);
        if (matcher.find()) {
            do {
                goods.add(s.substring(i, matcher.start()));
                bads.add(s.substring(matcher.start(), matcher.end()));
                i = matcher.end();
            } while (matcher.find());
            goods.add(s.substring(i, s.length()));
        } else {
            goods.add(s.substring(0, s.length()));
        }
        if (bads.size() + 1 != goods.size()) {
            throw new AssertionError("The number of bads plus one must equal the number of goods");
        }

        List<List<String>> tokenizedGoods = new ArrayList<>();
        for (String good : goods) {
            tokenizedGoods.add(unprotectedTokenize(good));
        }
        List<String> res = new ArrayList<>();
        for (int j = 0; j < bads.size(); j++) {
            res.addAll(tokenizedGoods.get(j));
            res.add(bads.get(j));
        }
        res.addAll(tokenizedGoods.get(tokenizedGoods.size()-1));
        res.removeIf(tok -> tok.length() == 0);
        return res;
    }

    protected static List<Integer> align(Tokenization toks, String orig) throws AlignmentFailed {
        int s_i = 0;
        List<String> tokens = toks.getTokens();
        int tlen = tokens.size();
        List<Integer> alignments = new ArrayList<>(tlen);
        for (int i = 0; i < tlen; i++) {
            alignments.add(null);
        }

        for (int tok_i=0; tok_i<tlen; tok_i++) {
            while (true) {
                int L = tokens.get(tok_i).length();
                if (s_i+L <= orig.length() && orig.substring(s_i, s_i+L).equals(tokens.get(tok_i))) {
                    alignments.set(tok_i, s_i);
                    s_i += L;
                    break;
                }
                s_i += 1;
                if (s_i >= orig.length()) {
                    throw new AlignmentFailed("Alignment failed: " + orig + ", " + toks + ", " + alignments);
                }
            }
        }

        if (alignments.contains(null)) {
            throw new AlignmentFailed("Alignment failed: " + orig + ", " + toks + ", " + alignments);
        }
        return alignments;
    }

    public String[] tokenize(String tweet) throws AlignmentFailed {
        String text = tweet;
        text = text.replaceAll("—", " ");
        text = text.replaceAll("(?<=\\w)/(\\s|$)", " ");
        text = squeezeWhitespace(text);
        // Convert HTML escape sequences into their actual characters (so that the tokenizer and emoticon finder are not fooled).
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&amp;", "&");

        Tokenization t = new Tokenization();
        t.addAll(simpleTokenize(text));
        t.text = new String(text);
        t.alignments = align(t,text);

        return t.getTokens().toArray(new String[0]);
    }
//    public static void main(String[] args) throws Exception {
//        String str = "I replace self.conv1() = nn.Conv2d(1, 32, 5, padding=2) with self.w_conv1 = Variable(torch.randn(1, 32, 5))";
//        String[] res = tokenize(str);
//        for (String s:res){
//            System.out.println("“"+s+"“");
//        }
//    }
}
