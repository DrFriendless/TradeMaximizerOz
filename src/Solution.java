import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 12/12/2010
 * Time: 11:25:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class Solution {
    private WantList wantList;
    private List<Cycle> cycles;
    private Graph graph;

    Solution(List<Cycle> cycles, Graph graph, WantList wantList) {
        this.cycles = cycles;
        this.graph = graph;
        this.wantList = wantList;
    }

    List<Cycle> getCycles() {
        return cycles;
    }

    Graph getGraph() {
        return graph;
    }

    WantList getWantList() {
        return wantList;
    }

    @Override
    public String toString() {
        int numTrades = 0;
        int numGames = 0;
        List<Integer> groupSizes = new ArrayList<Integer>();
        for (Cycle cycle : cycles) {
            int size = cycle.size();
            numTrades += size;
            groupSizes.add(size);
            for (Vertex v : cycle.getVertices()) {
                assert v.match != v.twin;
                if (!v.match.isMoney) numGames++;
            }
        }
        return "" + numTrades + " trades with " + numGames + " games " + groupSizes;
    }

    public long evaluate() {
        long score = 0;
        for (Cycle cycle : cycles) {
            score += cycle.getCost();
        }
        return score;
    }
}
