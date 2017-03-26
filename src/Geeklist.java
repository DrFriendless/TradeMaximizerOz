import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.net.URL;
import java.net.URLConnection;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * This class gives access to a geeklist on boardgamegeek to allow more data about the trade to be known to the
 * TradeMaximizer.
 *
 * @author John Farrell (friendless.farrell@gmail.com)
 */
public class Geeklist {
    private static final String URL = "https://boardgamegeek.com/xmlapi/geeklist/{0}?comments=1";
    private static final String FILENAME = "geeklist_{0}_page{1}.xml";
    private static final String CODE_RE = "1?\\d{DIGITS}-[A-Z0-9#\\-]{LETTERS}";

    private String id;
    private Pattern pattern;
    private List<GeeklistItem> items = new ArrayList<GeeklistItem>();
    private Map<String, GeeklistItem> itemsByCode = new HashMap<String, GeeklistItem>();
    private Map<String, GeeklistItem> itemsByItemId = new HashMap<String, GeeklistItem>();
    private Map<String, List<GeeklistItem>> itemsByUser = new HashMap<String, List<GeeklistItem>>();
    private List<String> otherUsers = new ArrayList<String>();
    private Set<String> errors;
    private int letters;

    public Geeklist(String id, int digits, int letters, Set<String> errors) throws Exception {
        this.id = id;
        this.pattern = Pattern.compile(CODE_RE.replace("DIGITS", Integer.toString(digits)).replace("LETTERS", Integer.toString(letters)));
        this.errors = errors;
        this.letters = letters;
        load();
    }

    public String getId() {
        return id;
    }

    private void load() throws Exception {
        File f = downloadFile();
        Document doc = parse(f);
        addItemsFromPage(doc);
    }

    private void addNewItem(GeeklistItem item) {
        items.add(item);
        String code = item.getTradeCode();
        if (code != null) {
            itemsByCode.put(code.toUpperCase(), item);
        } else {
            errors.add("No code for item " + item.getItemUrl());
        }
        itemsByItemId.put(item.getItemId(), item);
        String userName = item.getUserName();
        List<GeeklistItem> forUser = itemsByUser.get(userName);
        if (forUser == null) forUser = new ArrayList<GeeklistItem>();
        forUser.add(item);
        itemsByUser.put(userName, forUser);
    }

    private String deriveShortCode(String name) {
        Pattern p = Pattern.compile("[^0-9A-Z]+", Pattern.CASE_INSENSITIVE);
        String[] words = p.split(name);
        String[] subs = new String[] { "", "", "", "", "" };
        int total = 0;
        int position = 0;
        int codeLength = this.letters;
        while (total < codeLength) {
            //Write-Verbose "Position $position"
            Boolean addedCharacter = false;
            for (int index = 0; index < words.length; index++) {
                //Write-Verbose "Index $index"
                if (words[index].length() > position) {
                    subs[index] = words[index].substring(0, position + 1);
                    total++;
                    addedCharacter = true;
                }
                if (total >= 5) {
                    break;
                }
            }
            if (!addedCharacter) {
                break;
            }
            position++;
        }
        StringBuilder builder = new StringBuilder();
        for (String sub : subs) {
            builder.append(sub);
        }
        while (builder.length() < 5) {
            builder.append("X");
        }
        String code = builder.toString().toUpperCase();
        //System.out.println("Name '" + name + "' => '" + code + "'");
        return code;
    }

    private void addItemsFromPage(Document doc) {
        NodeList itemNodes = doc.getElementsByTagName("item");
        for (int i=0; i<itemNodes.getLength(); i++) {
            Element e = (Element) itemNodes.item(i);
            String gameName = e.getAttribute("objectname");
            String userName = e.getAttribute("username");
            String gameId = e.getAttribute("objectid");
            String itemId = e.getAttribute("id");
            NodeList comments = e.getElementsByTagName("comment");
            int commentCount = comments.getLength();
            String code = null;
            for (int j = 0; j < comments.getLength(); j++) {
                Element c = (Element) comments.item(j);
                c.normalize();
                String text = ((Text) c.getFirstChild()).getData();
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    code = matcher.group();
                    commentCount--;
                    break;
                }
            }
            if (code == null) {
                // If not found in comment, then auto-generate
                code = itemId + "-" + deriveShortCode(gameName);
            }
            GeeklistItem item = new GeeklistItem(this, itemId, gameName, userName, Integer.parseInt(gameId), code, i, commentCount);
            addNewItem(item);
        }
//        if (id.equals("156213")) {
//            int n = itemNodes.getLength();
//            // hack to fix up a massive cock-up in May 2013 - someone deleted a user's items
//            addNewItem(new GeeklistItem(this, "2612354-DWTCG", "Doctor Who: The Card Game", "ividdythou", 42, "2612354-DWTCG", n++, 0));
//            addNewItem(new GeeklistItem(this, "2612362-AQUAR", "Aqua Romana", "ividdythou", 42, "2612362-AQUAR", n++, 0));
//            addNewItem(new GeeklistItem(this, "2612360-WOWFA", "Wings of War: Famous Aces", "ividdythou", 42, "2612360-WOWFA", n++, 0));
//            addNewItem(new GeeklistItem(this, "2612367-HIGHC", "Highland Clans", "ividdythou", 42, "2612367-HIGHC", n++, 0));
//            addNewItem(new GeeklistItem(this, "2612372-TRIFO", "The Rivals for Catan", "ividdythou", 42, "2612372-TRIFO", n++, 0));
//        }
    }

    private void generateItemList() throws Exception {
        FileWriter f = new FileWriter("items.html");
        PrintWriter pw = new PrintWriter(f);
        pw.println("<HTML><BODY>");
        pw.println("<H1>BY USER</H1>");
        List<String> users = getUsers();
        for (String u : users) {
            List<GeeklistItem> userItems = itemsByUser.get(u);
            pw.println("<H2>" + u + "</H2>");
            for (GeeklistItem i : userItems) {
                pw.println(i.getItemHtml() + "<BR>");
            }
        }
        pw.println("</BODY></HTML>");
        pw.close();
        f.close();
    }

    private void generateHelp() throws Exception {
        FileWriter f = new FileWriter("items.txt");
        PrintWriter pw = new PrintWriter(f);
        List<String> users = getUsers();
        for (String u : users) {
            List<GeeklistItem> userItems = itemsByUser.get(u);
            for (GeeklistItem i : userItems) {
                pw.println("(" + u + ") " + i.getTradeCode() + ": ");
            }
            pw.println("(" + u + ") LIMIT: $0");
        }
        pw.close();
        f.close();
    }

    private void generateBruceList() throws Exception {
        FileWriter f = new FileWriter("bruce.txt");
        PrintWriter pw = new PrintWriter(f);
        List<String> users = getUsers();
        for (String u : users) {
            List<GeeklistItem> userItems = itemsByUser.get(u);
            for (GeeklistItem i : userItems) {
                pw.println(i.getTradeCode());
            }
        }
        pw.close();
        f.close();
    }

    public List<String> getUsers() {
        List<String> users = new ArrayList<String>(itemsByUser.keySet());
        users.addAll(otherUsers);
        Collections.sort(users, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.toLowerCase().compareTo(o2.toLowerCase());
            }
        });
        return users;
    }

    private static Document parse(File file) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        FileInputStream fis = new FileInputStream(file);
        return builder.parse(fis);
    }

    private static boolean containsItems(Document doc) {
        NodeList items = doc.getElementsByTagName("item");
        return items.getLength() > 0;
    }

    private File downloadFile() throws Exception {
        InputStream inStr = null;
        FileWriter fw = null;
        String filename = MessageFormat.format(FILENAME, id);
        String urlStr = MessageFormat.format(URL, id);
        System.err.println("Retrieving: " + urlStr);
        System.err.println("Saving to : " + filename);

        try {
            URL url = new URL(urlStr);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");

            HttpURLConnection httpConn = (HttpURLConnection) connection;
            HttpURLConnection.setFollowRedirects(true);

            int responseCode = httpConn.getResponseCode();
            String responseMessage = httpConn.getResponseMessage();
            System.err.println("Response: " + responseCode + " " + responseMessage);

            String encoding = httpConn.getContentEncoding();
            System.err.println("Encoding: " + encoding);
            if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                inStr = new GZIPInputStream(httpConn.getInputStream());
            } else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
                inStr = new InflaterInputStream(httpConn.getInputStream(),
                        new Inflater(true));
            } else {
                inStr = httpConn.getInputStream();
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(inStr));

            fw = new FileWriter(filename);
            BufferedWriter bw = new BufferedWriter(fw);

            String line;
            while ((line = br.readLine()) != null) {
                bw.write(line);
                bw.newLine();
            }

            bw.flush();
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        inStr.close();
        fw.close();

        System.err.println("Done");
        return new File(filename);
    }

    private void checkAllItemsHaveCodes() {
        boolean ok = true;
        for (GeeklistItem item : items) {
            if (item.getTradeCode() == null) {
                System.out.println("No trade code for item " + item.getGeekListLocation() + " " + item.getItemUrl());
                ok = false;
            }
        }
        if (ok) System.out.println("All items have valid trade codes.");
    }

    List<String> getAllCodes() {
        return new ArrayList<String>(itemsByCode.keySet());
    }

    List<String> getAllItemIds() {
        return new ArrayList<String>(itemsByItemId.keySet());
    }

    List<String> getAllUsers() {
        return new ArrayList<String>(itemsByUser.keySet());
    }

    GeeklistItem getItem(String code) {
        GeeklistItem item = itemsByCode.get(code.toUpperCase());
        // Replace with generated IDs
        /*
        // Default to finding items by ID (if not found by code)
        if (item == null) {
            int dashPosition = code.indexOf('-');
            if (dashPosition > 0) {
                String itemId = code.substring(0, dashPosition);
                item = getItemById(itemId);
            }
        }
        */
        return item;
    }

    GeeklistItem getItemById(String itemId) {
        return itemsByItemId.get(itemId);
    }

    GeeklistItem getItemForVertex(Vertex v) {
        String code = v.name;
        if (code.indexOf(' ') > 0) code = code.substring(0, code.indexOf(' '));
        return getItem(code);
    }

    public static void main(String[] args) throws Exception {
        Geeklist geeklist = new Geeklist("172859", 6, 5, new HashSet<String>());
        geeklist.checkAllItemsHaveCodes();
        geeklist.generateItemList();
        geeklist.generateHelp();
        geeklist.generateBruceList();
    }

    public void setWantListUsers(List<String> wantListUsers) {
        for (String u : wantListUsers) {
            if (!itemsByUser.containsKey(u) && !otherUsers.contains(u)) {
                otherUsers.add(u);
            }
        }
    }
}
