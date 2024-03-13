import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Properties;

public class LuceneCodeIndexer {
    private static String config_file = "./config/file_structure.ini";
    private static String exp_path = "";
    private static String eval_path = "";
    private static String code_xml_dir = "";
    private static String index_dir = "";

    /**
     * Build Index for a set of code snippets.
     */
    public static void buildIndex4CodeXmls(String codes_path, String index_path) {
        // judge if the path is a directory
        final Path docDir = Paths.get(codes_path);
        if (!Files.isDirectory(docDir))
            return;

        // create the index directory
        File f = new File(index_path);
        if (!f.exists())
            f.mkdirs();

        // index all code xml files
        try {
            Directory dir = FSDirectory.open(Paths.get(index_path));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(dir, iwc);
            Files.walkFileTree(docDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    indexCodeXml(writer, file);
                    return FileVisitResult.CONTINUE;
                }
            });
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Index a code xml.
     */
    private static void indexCodeXml(IndexWriter writer, Path file) {
        try {
            System.out.println("Indexing file: " + file.getFileName());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document xmlDoc = builder.parse(Files.newInputStream(file));
            NodeList nodeList = xmlDoc.getElementsByTagName("code");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String codeId = element.getAttribute("CodeId");
                    String postId = element.getAttribute("PostId");
                    String postTypeId = element.getAttribute("PostTypeId");
                    String code = element.getAttribute("Code");

                    // create Lucene Document & set fields
                    Document luceneDoc = new Document();
                    luceneDoc.add(new StoredField("CodeId", codeId));
                    luceneDoc.add(new StoredField("PostId", postId));
                    luceneDoc.add(new StoredField("PostTypeId", postTypeId));
                    luceneDoc.add(new TextField("Code", code, Field.Store.YES));

                    // add document to Lucene Index
                    writer.addDocument(luceneDoc);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Search the top-k similar cides for queries based on the Lucene index.
     * 
     * @param index_path: the path that contains the Lucene index built for codes.
     * @param query_file: a file that contains query code.
     * @param k:          the top k value.
     * @param saveScore:  whether save the score (i.e., similarity) of each
     *                    retrieved document.
     * @param res_path:   the path to store the top k similar code snippets of each
     *                    query.
     */
    public static void search(String index_path, String query_file, int k, boolean saveScore, String res_path) {
        try {
            System.out.println("Search for query: " + query_file);
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index_path)));
            IndexSearcher searcher = new IndexSearcher(reader);
            // use BM25 as the similarity function
            searcher.setSimilarity(new BM25Similarity());
            Analyzer analyzer = new StandardAnalyzer();
            QueryParser parser = new QueryParser("Code", analyzer); // Code is the search field
            // get query code string
            String query_code = readFileAsString(query_file);
            // query for top-k similar code snippets
            String escaped_query_code = QueryParser.escape(query_code);
            Query query;
            try {
                query = parser.parse(escaped_query_code); // escape the special characters in the query code, in case
                                                          // it will be parsed as syntax element of Lucene query
            } catch (ParseException e) {
                escaped_query_code = escaped_query_code.substring(0, 4096);
                query = parser.parse(escaped_query_code);
            }
            TopDocs results = searcher.search(query, k);
            long hit_num = Math.min(k, results.totalHits.value);
            System.out.println(results.totalHits + " total matching code snippets");

            ScoreDoc[] hits = results.scoreDocs;

            float maxscore = 0.0f;
            for (int i = 0; i < k; i++) {
                if (hits[i].score > maxscore)
                    maxscore = hits[i].score;
            }

            // String topks = "";
            // for (int i = 0; i < hit_num; i++) {
            // Document doc = searcher.doc(hits[i].doc);
            // String CodeId = doc.get("CodeId");
            // String PostId = doc.get("PostId");
            // String Code = doc.get("Code");

            // if (saveScore) {
            // topks += CodeId + "\t" + PostId + "\t" + hits[i].score / maxscore + "\n";
            // topks += Code + "\n";
            // } else {
            // topks += CodeId + "\t" + PostId + "\n";
            // topks += Code + "\n";
            // }

            // }
            // bw.write(topks.trim());

            // write the top-k similar code snippets to a directory
            String save_dir = res_path; // + "/" + Paths.get(query_file).getFileName().toString().replace(".java", "");
            Path dirPath = Paths.get(res_path);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            } else {
                // delete all files in the directory
                Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            for (int i = 0; i < hits.length; i++) {
                Document doc = searcher.doc(hits[i].doc);
                String code = doc.get("Code");
                String codeId = doc.get("CodeId");
                String postId = doc.get("PostId");
                float score = hits[i].score;

                String fileName = codeId + "_" + postId + "_" + hits[i].score / maxscore + ".java";
                Path filePath = Paths.get(save_dir, fileName);
                Files.write(filePath, code.getBytes(StandardCharsets.UTF_8));
            }
            // print the save_dir
            System.out.println("Save top-k similar code snippets to: " + save_dir);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String readFileAsString(String fileName) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(fileName)));
        return content;
    }

    protected static void read_config() throws Exception {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(config_file));
            exp_path = properties.getProperty("EXP_PATH");
            // eval_path = "/home/zhangn279/laj/CK4TI/Evaluation";
            code_xml_dir = exp_path + properties.getProperty("SO_CODE_FOLDER");
            index_dir = exp_path + properties.getProperty("CODE_LUCENE_INDEX");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * offline version:
     * build lucene index for code snippets from SO.
     * online version:
     * given a code snippet, search top_k similar code snippets by lucene index,
     * then save results to a folder.
     * **need arguments**:
     * - code_snippet_path: path of code snippet
     * - lucene_top_k: number of results to save
     * - res_path: folder to save results
     */
    public static void main(String[] args) throws Exception {
        long start = 0l, end = 0l;
        read_config(); // get directory of code xmls
        int arg_len = args.length;
        if (arg_len < 1) {
            throw new IllegalArgumentException("class must has at least one argument!");
        }
        String mode = args[0];
        if (mode.equals("-offline")) {
            // Step 1: build index for code xmls
            start = System.currentTimeMillis();
            buildIndex4CodeXmls(code_xml_dir, index_dir);
            end = System.currentTimeMillis();
            System.out.println("Time Cost:" + (end - start) + "ms"); // Time
            // Cost:362714ms
        } else if (mode.equals("-online")) {
            // Step 2: search for the top k similar code snippets for queries
            if (arg_len < 2) {
                throw new IllegalArgumentException("missing argument: code_snippet_path");
            }
            if (arg_len < 3) {
                throw new IllegalArgumentException("missing argument: lucene_top_k");
            }
            if (arg_len < 4) {
                throw new IllegalArgumentException("missing argument: res_path");
            }
            String query_file = exp_path + args[1];
            int topk = Integer.parseInt(args[2]);
            String res_path = args[3];
            start = System.currentTimeMillis();

            search(index_dir, query_file, topk, true, res_path);
            end = System.currentTimeMillis();
            // System.out.println("Time Cost:" + (end - start) + "ms");
        }
    }
}
