import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
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
import java.util.HashMap;
import java.util.Properties;

public class LucenePostIndexer {
    private static String config_file = "./config/path_config.ini";
    private static String exp_path = "";
    private static String post_dump_dic = "";
    private static String index_dir = "";
    private static boolean split_QA = false;

    public static void buildIndex4Posts(String posts_path, String index_path) {
        // judge if the path is a directory
        final Path docDir = Paths.get(posts_path);
        if (!Files.isDirectory(docDir)) return;

        // create the index directory
        File f = new File(index_path);
        if (!f.exists()) f.mkdirs();

        // create the path dictionary
        HashMap<String, String> pathDict = new HashMap<>();
        pathDict.put("question", posts_path + "/questions");
        pathDict.put("answer", posts_path + "/answers");

        // index all Posts files
        try {
            Directory index_dir = FSDirectory.open(Paths.get(index_path));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(index_dir, iwc);
            for (String key : pathDict.keySet()) {
                Files.walkFileTree(Paths.get(pathDict.get(key)), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        switch (key) {
                            case "question":
                                indexQuestionXml(writer, file);
                                break;
                            case "answer":
                                indexAnswerXml(writer, file);
                                break;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void indexQuestionXml(IndexWriter writer, Path file) {
        try {
            System.out.println("Indexing dir:" + file.getParent().toString() + "\tfile: " + file.getFileName());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document xmlDoc = builder.parse(Files.newInputStream(file));
            NodeList nodeList = xmlDoc.getElementsByTagName("row");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    int postId = Integer.parseInt(element.getAttribute("Id"));
                    int postTypeId = 1; // 1: question, 2: answer, 3: comment
                    String Body = element.getAttribute("Body");

                    // create Lucene Document and add fields
                    Document luceneDoc = new Document();
                    luceneDoc.add(new IntField("PostId", postId, Field.Store.YES));
                    luceneDoc.add(new StoredField("PostTypeId", postTypeId));
                    luceneDoc.add(new StoredField("Body", Body));
                    // write doc to Lucene index
                    writer.addDocument(luceneDoc);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void indexAnswerXml(IndexWriter writer, Path file) {
        try {
            System.out.println("Indexing dir:" + file.getParent().toString() + "\tfile: " + file.getFileName());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document xmlDoc = builder.parse(Files.newInputStream(file));
            NodeList nodeList = xmlDoc.getElementsByTagName("row");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    int postId = (split_QA)? Integer.parseInt(element.getAttribute("Id")):Integer.parseInt(element.getAttribute("ParentId"));
                    int postTypeId = 2; // 1: question, 2: answer, 3: comment
                    String Body = element.getAttribute("Body");

                    // create Lucene Document and add fields
                    Document luceneDoc = new Document();
                    luceneDoc.add(new IntField("PostId", postId, Field.Store.YES));
                    luceneDoc.add(new StoredField("PostTypeId", postTypeId));
                    luceneDoc.add(new StoredField("Body", Body));
                    // write doc to Lucene index
                    writer.addDocument(luceneDoc);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Search the post content by PostId, including question, answer and comment.
     * 
     * @param PostId:     the id of the post.
     * @param index_path: the path that contains the Lucene index built for codes.
     * @param res_dir:    the path to store the post content (in json format).
     */
    public static void search(int PostId, String index_path, String res_dir) {
        try {
            Path res_dir_path = Paths.get(res_dir);
            if (!Files.exists(res_dir_path))
                Files.createDirectories(res_dir_path);

            System.out.println("Search for Post: " + PostId);
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index_path)));
            IndexSearcher searcher = new IndexSearcher(reader);

            Query query = IntField.newExactQuery("PostId", PostId);
            TopDocs results = searcher.search(query, Integer.MAX_VALUE);
            long hit_num = results.totalHits.value;
            System.out.println(hit_num + " total matching content");

            StringBuilder question = new StringBuilder("Qustion:\n");
            StringBuilder answer = new StringBuilder();

            int ansnum = 0;
            for (ScoreDoc scoreDoc : results.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                int type = Integer.parseInt(doc.get("PostTypeId"));
                switch (type) {
                    case 1: // question
                        question.append(doc.get("Body"));
                        break;
                    case 2: // answer
                        ansnum += 1;
                        answer.append("Answer ").append(ansnum).append(":\n").append(doc.get("Body"));
                        break;
                }
            }

            String save_json = res_dir + "/" + PostId + ".txt";
            FileWriter file = new FileWriter(save_json, StandardCharsets.UTF_8);
            file.write(question.toString());
            file.write(answer.toString());
            file.close();
            System.out.println("Successfully saved post body to:" + save_json);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void read_config() throws Exception {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(config_file));
            post_dump_dic = properties.getProperty("POST_DUMP_DIC");
            index_dir = properties.getProperty("POST_LUCENE_INDEX");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * offline version:
     * build lucene index for posts from SO.
     * **need arguments**:
     * - split_QA: "True" or "False"
     * online version:
     * given a post_id, search related content of the post by lucene index,
     * including question, answer and comments
     * then save a json object as the result to a file.
     * **need arguments**:
     * - post_id: id of the post
     * - result_folder: path of the file saving the result
     */
    public static void main(String[] args) throws Exception {
        long start = 0l, end = 0l;
        read_config();
        int arg_len = args.length;
        if (arg_len < 1) {
            throw new IllegalArgumentException("class must has at least one argument!");
        }
        String mode = args[0];
        if (mode.equals("-offline")) {
            // Step 1: build index for Posts
            if (arg_len < 2) {
                throw new IllegalArgumentException("missing argument: split_QA");
            }
            if(args[1].equals("True")) split_QA = true;
//            start = System.currentTimeMillis();
            buildIndex4Posts(post_dump_dic, index_dir);
//            end = System.currentTimeMillis();
//            System.out.println("Time Cost:" + (end - start) + "ms"); // 319597ms (5.33min)
        } else if (mode.equals("-online")) {
            // Step 2: get post content by PostId
            if (arg_len < 2) {
                throw new IllegalArgumentException("missing argument: post_id");
            }
            if (arg_len < 3) {
                throw new IllegalArgumentException("missing argument: result_folder");
            }
            int PostId = Integer.parseInt(args[1]);
            String res_path = exp_path + args[2];
            search(PostId, index_dir, res_path);
        } else {
            throw new IllegalArgumentException("invalid argument!");
        }
    }
}
