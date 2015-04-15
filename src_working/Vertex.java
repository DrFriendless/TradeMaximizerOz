import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 11/12/2010
 * Time: 12:35:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class Vertex {
    String name;
    String user;
    boolean isDummy;
    boolean isMoney;
    Graph.VertexType type;
    /** The TradeRequest which caused this vertex to be created. */
    TradeRequest request;
    double pricePaid;

    Vertex(String name, String user, boolean isDummy, boolean isMoney, Graph.VertexType type, TradeRequest request) {
        this.name = name;
        this.user = user;
        this.isDummy = isDummy;
        this.isMoney = isMoney;
        this.type = type;
        this.request = request;
        this.pricePaid = 0;
    }

    TradeRequest getTradeRequest() {
        return request;
    }

    void setPricePaid(double dollars) {
        this.pricePaid = dollars;
    }

    String summary() {
        if (isMoney) {
            return "$" + pricePaid + " from " + request.getUserName();
        } else {
            return request.getItemSummary();
        }
    }

    List<Edge> edges = new ArrayList<Edge>();
    Edge[] EDGES;

    // internal data for graph algorithms
    long minimumInCost = Long.MAX_VALUE; // only kept in the senders
    Vertex twin;
    int mark = 0; // used for marking as visited in dfs and dijkstra
    Vertex match = null;
    long matchCost = 0;
    Vertex from = null;
    long price = 0;
    Heap.Entry heapEntry = null;
    int component = 0;
    boolean used = false;

    Vertex savedMatch = null;
    long savedMatchCost = 0;

    public double getPricePaid() {
        return pricePaid;
    }

    @Override
    public String toString() {
        return "Vertex[" + request + "]";
    }
}
