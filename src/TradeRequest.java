import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 04/12/2010
 * Time: 6:52:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class TradeRequest implements Comparable<TradeRequest> {
    private static int id;
    private String user;
    private String item;
    private List<String> willAccept;
    private int amount;
    private int salePrice = 0;

    TradeRequest(String user, String item, List<String> willAccept) {
        if (user.startsWith("(")) user = user.substring(1);
        if (user.endsWith(")")) user = user.substring(0, user.length()-1);
        this.user = user;
        this.item = item;
        this.willAccept = new ArrayList<String>(willAccept);
        if (isDummy()) {
            this.item = this.item + " for " + this.user;
        } else if (isMoney()) {
            this.amount = Money.parseMoney(item);
            this.item = this.item + " bid " +  id++;
        }
        int moneyCount = 0;
        if (isMoney()) moneyCount++;
        if (isDummy()) moneyCount++;
        for (String a : this.willAccept) {
            if (a.startsWith("$")) {
                moneyCount++;
                salePrice = Money.parseMoney(a);
            }
        }
        if (moneyCount > 1) throw new RuntimeException("too complicated");
    }

    String getUserName() {
        return user;
    }
    
    void setUserName(String u) {
        this.user = u;
    }

    String getItemName() {
        return item;
    }

    boolean isDummy() {
        return item.startsWith("%");
    }

    boolean isMoney() {
        return item.startsWith("$");
    }

    boolean isLimit() {
        return item.equalsIgnoreCase("LIMIT");
    }

    List<String> willAccept() {
        return new ArrayList<String>(willAccept);
    }

    @Override
    public String toString() {
        return "(" + user + ") " + item + ": " + willAccept;
    }

    int getAmount() {
        return amount;
    }

    public int getSalePrice() {
        return salePrice;
    }

    public String getItemSummary() {
        if (isMoney()) {
            return "money from " + user;
        } else {
            return item + " from " + user;
        }
    }

    public int compareTo(TradeRequest o) {
        if (o == null) return -1;
        return item.compareTo(o.item);
    }
}
