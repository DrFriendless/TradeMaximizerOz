import java.text.*;

/**
 * Information found about an item in the geeklist.
 *
 * @author John Farrell (friendless.farrell@gmail.com)
 */
public class GeeklistItem {
    private static String URL_PATTERN = "http://www.boardgamegeek.com/geeklist/{0}/item/{1}#item{2}";
    private static String HTML_PATTERN = "<A HREF=\"{0}\">{1}</A>";
    private String gameName;
    private String userName;
    private int gameId;
    private String tradeCode;
    private int itemNumber;
    private Geeklist list;
    private String itemId;
    private int comments;

    public GeeklistItem(Geeklist list, String itemId, String gameName, String userName, int gameId, String tradeCode, int itemNumber, int comments) {
        this.list = list;
        this.itemId = itemId;
        this.gameName = gameName;
        this.userName = userName;
        this.gameId = gameId;
        this.tradeCode = tradeCode;
        this.itemNumber = itemNumber;
        this.comments = comments;
    }

    public boolean isSameGame(GeeklistItem other) {
        return gameId == other.gameId;
    }

    public String getTradeCode() {
        return tradeCode;
    }

    public String getGeekListLocation() {
        return "Entry " + (itemNumber+1) + ": " + gameName;
    }

    public String getItemUrl() {
        return MessageFormat.format(URL_PATTERN, list.getId(), itemId, itemId);
    }

    public String getItemHtml() {
        String s = MessageFormat.format(HTML_PATTERN, getItemUrl(), getGeekListLocation());
        if (tradeCode != null) {
            s = s + " (" + tradeCode + ")";
        }
        // report non-trade-code comments in case it's a question for the trader that they should look at.
        if (comments > 0) {
            s = s + " (" + comments + " comments)";
        }
        return s;
    }

    public String getNameHtml() {
        String s = MessageFormat.format(HTML_PATTERN, getItemUrl(), gameName);
        return s;
    }

    public String getUserName() {
        return userName;
    }

    public String getName() {
        return gameName;
    }

    public boolean isMoney() {
        return false;
    }

    @Override
    public String toString() {
        return gameName + " from " + userName;
    }
}
