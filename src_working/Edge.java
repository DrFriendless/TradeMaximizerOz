/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 11/12/2010
 * Time: 12:36:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class Edge {
    Vertex receiver;
    Vertex sender;
    long cost;
    TradeRequest request;

    Edge(Vertex receiver,Vertex sender,long cost, TradeRequest request) {
        assert receiver.type == Graph.VertexType.RECEIVER;
        assert sender.type == Graph.VertexType.SENDER;
        this.receiver = receiver;
        this.sender = sender;
        this.cost = cost;
        this.request = request;
    }

    private Edge() {} // hide default constructor
}
