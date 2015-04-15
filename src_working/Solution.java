import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 12/12/2010
 * Time: 11:25:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class Solution {
    private List<Cycle> cycles;
    private Graph graph;

    Solution(List<Cycle> cycles, Graph graph) {
        this.cycles = cycles;
        this.graph = graph;
    }

    List<Cycle> getCycles() {
        return cycles;
    }

    Graph getGraph() {
        return graph;
    }
}
