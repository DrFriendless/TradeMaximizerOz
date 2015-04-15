import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 11/12/2010
 * Time: 11:37:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class Cycle {
    private List<Vertex> vertices;
    private long cost;

    Cycle(List<Vertex> vertices, long cost) {
        this.vertices = vertices;
        this.cost = cost;
    }

    int size() {
        return vertices.size();
    }

    List<Vertex> getVertices() {
        return new ArrayList<Vertex>(vertices);
    }

    long getCost() {
        return cost;
    }
}
