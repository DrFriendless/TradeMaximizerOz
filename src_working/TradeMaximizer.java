// TradeMaximizer.java
// Created by Chris Okasaki (cokasaki)
// Version 1.3a
// $LastChangedDate: 2008-03-07 09:08:08 -0500 (Fri, 07 Mar 2008) $
// $LastChangedRevision: 28 $

import java.io.*;
import java.util.*;

public class TradeMaximizer {
    public static void main(String[] args) throws IOException {
        new TradeMaximizer().run(args[0]);
    }

    final String version = "Version 1.3.Friendless.b";

    void run(String filename) throws IOException {
        System.out.println("TradeMaximizer " + version);
        File inputFile = new File(filename);
        File htmlResultFile = new File(inputFile.getParentFile(), "result.html");
        File textResultFile = new File(inputFile.getParentFile(), "result.txt");
        PrintStream out = new PrintStream(new FileOutputStream(textResultFile));
        Set<String> errors = new HashSet<String>();
        List<TradeRequest> wantLists = readWantLists(filename);
        if (wantLists == null) return;
        WantList wantList = new WantList(wantLists, errors);
        if (options.size() > 0) {
            out.print("Options:");
            for (String option : options) out.print(" "+option);
            out.println();
        }
        out.println();

        Geeklist geeklist = null;
        if (geeklistId != null && codeDigits > 0 && codeLetters > 0) {
            try {
                geeklist = new Geeklist(geeklistId, codeDigits, codeLetters, errors);
                wantList.setGeekList(geeklist, errors);
                List<String> wantListUsers = new ArrayList<String>();
                for (TradeRequest tr : wantLists) {
                    if (!wantListUsers.contains(tr.getUserName())) wantListUsers.add(tr.getUserName());
                }
                geeklist.setWantListUsers(wantListUsers);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        wantList.check(errors, out);
        Solution solution;
        while (true) {
            Solution best = findSolution(wantList, errors);
            Map<String, Double> paid = adjustSalePrices(best.getCycles());
            String userPaidTooMuch;
            if ((userPaidTooMuch = wantList.satisfiesSpendingLimits(paid)) == null) {
                solution = best;
                break;
            } else {
                // what a saga - must deterministically remove an entry so the program produces same result each time
                Map<TradeRequest, Double> purchases = findPurchases(userPaidTooMuch, best.getCycles());
                double min = Collections.min(purchases.values());
                for (Map.Entry<TradeRequest, Double> entry : purchases.entrySet()) {
                    if (entry.getValue() != min) {
                        wantList.removeRequest(entry.getKey());
                    }
                }
                List<TradeRequest> rs = new ArrayList<TradeRequest>(purchases.keySet());
                Collections.sort(rs);
                for (Map.Entry<TradeRequest, Double> entry : purchases.entrySet()) {
                    if (entry.getKey() == rs.get(0)) {
                        wantList.removeRequest(entry.getKey());
                        break;
                    }
                }
            }
        }

        displayMatches(solution, out);
        if (geeklist != null) {
            try {
                wantList.showUnsubmitted();
                displayMatchesHtml(solution, geeklist, wantList, htmlResultFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        out.close();
        displayErrors(errors);
    }

    private void displayErrors(Set<String> errorLines) {
        if (errorLines.size() > 0) {
            System.out.println();
            List<String> errors = new ArrayList<String>(errorLines);
            Collections.sort(errors);
            System.out.println("ERRORS and WARNINGS");
            for (String e : errors) {
                System.out.println(e);
            }
            System.out.println();
        }
    }

    private Map<TradeRequest, Double> findPurchases(String user, List<Cycle> cycles) {
        Map<TradeRequest, Double> prices = new HashMap<TradeRequest, Double>();
        for (Cycle c : cycles) {
            for (Vertex v : c.getVertices()) {
                TradeRequest request = v.getTradeRequest();
                if (request.isMoney()) {
                    if (user.equals(v.getTradeRequest().getUserName())) {
                        double amount = v.getPricePaid();
                        prices.put(v.getTradeRequest(), amount);
                    }
                }
            }
        }
        return prices;
    }

    private Map<String, Double> adjustSalePrices(List<Cycle> cycles) {
        Map<String, Double> dollarsPaid = new HashMap<String, Double>();
        for (Cycle c : cycles) {
            adjustSalePrices(c, dollarsPaid);
        }
        return dollarsPaid;
    }

    private void adjustSalePrices(Cycle cycle, Map<String, Double> dollarsPaid) {
        for (Vertex v : cycle.getVertices()) {
            TradeRequest request = v.getTradeRequest();
            if (request.isMoney()) {
                String user = v.getTradeRequest().getUserName();
                double price = v.twin.match.getTradeRequest().getSalePrice();
                v.setPricePaid(price);
                v.twin.setPricePaid(price);
                if (dollarsPaid.get(user) == null) {
                    dollarsPaid.put(user, price);
                } else {
                    dollarsPaid.put(user, dollarsPaid.get(user) + price);
                }
            }
        }
    }

    private Solution findSolution(WantList wantList, Set<String> errors) {
        Graph graph = buildGraph(wantList, errors);

        graph.removeImpossibleEdges();
        List<Cycle> bestCycles = graph.findCycles();
        int bestSumSquares = sumOfSquares(bestCycles);
        if (iterations > 1) {
            graph.saveMatches();
            for (int i = 1; i < iterations; i++) {
                graph.shuffle();
                List<Cycle> cycles = graph.findCycles();
                int sumSquares = sumOfSquares(cycles);
                if (sumSquares < bestSumSquares) {
                    bestSumSquares = sumSquares;
                    bestCycles = cycles;
                    graph.saveMatches();
                    int[] groups = new int[cycles.size()];
                    for (int j = 0; j < cycles.size(); j++) {
                        groups[j] = cycles.get(j).size();
                    }
                    Arrays.sort(groups);
                    int tot = 0;
                    System.out.print("[ "+sumSquares + " :");
                    for (int j = groups.length-1; j >= 0; j--) {
                        System.out.print(" " + groups[j]);
                        tot = tot + groups[j];
                    }
                    System.out.println(" ] " + tot);
                }
            }
            System.out.println();
            graph.restoreMatches();
        }
        return new Solution(bestCycles, graph);
    }

    int sumOfSquares(List<Cycle> cycles) {
        int sum = 0;
        for (Cycle cycle : cycles) sum += cycle.size()*cycle.size();
        return sum;
    }

    boolean requireColons = true;
    boolean showRepeats = true;
    boolean showNonTrades = true;
    boolean allowDummies = true;

    static final int NO_PRIORITIES = 0;
    static final int LINEAR_PRIORITIES = 1;
    static final int TRIANGLE_PRIORITIES = 2;
    static final int SQUARE_PRIORITIES = 3;
    static final int SCALED_PRIORITIES = 4;
    static final int EXPLICIT_PRIORITIES = 5;

    int priorityScheme = NO_PRIORITIES;
    int smallStep = 1;
    int bigStep = 9;
    long nonTradeCost = 1000000000L; // 1 billion

    int iterations = 1;

    String geeklistId = null;
    int codeDigits = 0;
    int codeLetters = 0;

    List<String> options = new ArrayList<String>();

    List<TradeRequest> readWantLists(String filename) {
        String line = null;
        int lineNumber = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            List<TradeRequest> wantLists = new ArrayList<TradeRequest>();
            boolean readingOfficialNames = false;

            for (lineNumber = 1;;lineNumber++) {
                line = in.readLine();
                if (line == null) return wantLists;
                line = line.trim();
                if (line.length() == 0) continue; // skip blank link
                if (line.matches("#!.*")) { // declare options
                    if (wantLists.size() > 0)
                        fatalError("Options (#!...) cannot be declared after first real want list", lineNumber);
                    for (String option : line.toUpperCase().substring(2).trim().split("\\s+")) {
                        if (option.equals("REQUIRE-COLONS"))
                            requireColons = true;
                        else if (option.equals("REQUIRE-USERNAMES"))
                            ; // requireUserNames is ALWAYS true
                        else if (option.equals("ALLOW-DUMMIES"))
                            allowDummies = true;
                        else if (option.equals("LINEAR-PRIORITIES"))
                            priorityScheme = LINEAR_PRIORITIES;
                        else if (option.equals("TRIANGLE-PRIORITIES"))
                            priorityScheme = TRIANGLE_PRIORITIES;
                        else if (option.equals("SQUARE-PRIORITIES"))
                            priorityScheme = SQUARE_PRIORITIES;
                        else if (option.equals("SCALED-PRIORITIES"))
                            priorityScheme = SCALED_PRIORITIES;
                        else if (option.equals("EXPLICIT-PRIORITIES"))
                            priorityScheme = EXPLICIT_PRIORITIES;
                        else if (option.startsWith("GEEKLIST="))
                            geeklistId = option.substring(9);
                        else if (option.startsWith("CODE-DIGITS="))
                            codeDigits = Integer.parseInt(option.substring(12));
                        else if (option.startsWith("CODE-LETTERS="))
                            codeLetters = Integer.parseInt(option.substring(13));
                        else if (option.startsWith("SMALL-STEP=")) {
                            String num = option.substring(11);
                            if (!num.matches("\\d+"))
                                fatalError("SMALL-STEP argument must be a non-negative integer",lineNumber);
                            smallStep = Integer.parseInt(num);
                        }
                        else if (option.startsWith("BIG-STEP=")) {
                            String num = option.substring(9);
                            if (!num.matches("\\d+"))
                                fatalError("BIG-STEP argument must be a non-negative integer",lineNumber);
                            bigStep = Integer.parseInt(num);
                        }
                        else if (option.startsWith("NONTRADE-COST=")) {
                            String num = option.substring(14);
                            if (!num.matches("[1-9]\\d*"))
                                fatalError("NONTRADE-COST argument must be a positive integer",lineNumber);
                            nonTradeCost = Long.parseLong(num);
                        }
                        else if (option.startsWith("ITERATIONS=")) {
                            String num = option.substring(11);
                            if (!num.matches("[1-9]\\d*"))
                                fatalError("ITERATIONS argument must be a positive integer",lineNumber);
                            iterations = Integer.parseInt(num);
                        } else {
                            fatalError("Unknown option \""+option+"\"",lineNumber);
                        }
                        options.add(option);
                    }
                    continue;
                }
                if (line.matches("#.*")) continue; // skip comment line
                if (line.indexOf("#") != -1) {
                    if (readingOfficialNames) {
                        if (line.split("[:\\s]")[0].indexOf("#") != -1) {
                            fatalError("# symbol cannot be used in an item name",lineNumber);
                        }
                    }
                    else {
                        fatalError("Comments (#...) cannot be used after beginning of line",lineNumber);
                    }
                }

                // check parens for user name
                if (line.indexOf("(") == -1)
                    fatalError("Missing username with REQUIRE-USERNAMES selected",lineNumber);
                if (line.charAt(0) == '(') {
                    if (line.lastIndexOf("(") > 0)
                        fatalError("Cannot have more than one '(' per line",lineNumber);
                    int close = line.indexOf(")");
                    if (close == -1)
                        fatalError("Missing ')' in username",lineNumber);
                    if (close == line.length()-1)
                        fatalError("Username cannot appear on a line by itself",lineNumber);
                    if (line.lastIndexOf(")") > close)
                        fatalError("Cannot have more than one ')' per line",lineNumber);
                    if (close == 1)
                        fatalError("Cannot have empty parentheses",lineNumber);
                }else if (line.indexOf("(") > 0) {
                    fatalError("Username can only be used at the front of a want list",lineNumber);
                } else if (line.indexOf(")") > 0) {
                    fatalError("Bad ')' on a line that does not have a '('",lineNumber);
                }

                // check semicolons
                line = line.replaceAll(";"," ; ");
                int semiPos = line.indexOf(";");
                if (semiPos != -1) {
                    if (semiPos < line.indexOf(":"))
                        fatalError("Semicolon cannot appear before colon",lineNumber);
                    String before = line.substring(0,semiPos).trim();
                    if (before.length() == 0 || before.charAt(before.length()-1) == ')')
                        fatalError("Semicolon cannot appear before first item on line", lineNumber);
                }

                // check and remove colon
                int colonPos = line.indexOf(":");
                if (colonPos != -1) {
                    if (line.lastIndexOf(":") != colonPos)
                        fatalError("Cannot have more that one colon on a line",lineNumber);
                    String header = line.substring(0,colonPos).trim();
                    if (!header.matches("(.*\\)\\s+)?[^(\\s)]\\S*"))
                        fatalError("Must have exactly one item before a colon (:)",lineNumber);
                    line = line.replaceFirst(":"," "); // remove colon
                } else if (requireColons) {
                    fatalError("Missing colon with REQUIRE-COLONS selected",lineNumber);
                }
                int close = line.indexOf(")");
                String name = line.substring(0, close);
                line = line.substring(close+1);
                String[] fields = line.trim().split("\\s+");
                if (fields.length < 1) {
                    System.out.println("INVALID: " + line);
                } else {
                    wantLists.add(new TradeRequest(name, fields[0], Arrays.asList(fields).subList(1, fields.length)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fatalError("Error parsing line " + lineNumber + ": " + line);
            return null;
        }
    }

    void fatalError(String msg) {
        System.out.println();
        System.out.println("FATAL ERROR: " + msg);
        System.exit(1);
    }

    void fatalError(String msg,int lineNumber) {
        fatalError(msg + " (line " + lineNumber + ")");
    }

    final long UNIT = 1L;

    private List<String> findSuitableMoneyBids(double amount, String sellingUser, Map<Double, List<TradeRequest>> moneyAmounts) {
        List<String> result = new ArrayList<String>();
        for (Map.Entry<Double, List<TradeRequest>> entry : moneyAmounts.entrySet()) {
            if (entry.getKey() >= amount) {
                for (TradeRequest tr : entry.getValue()) {
                    // you can't sell to yourself, so ignore bids from the same user
                    if (!tr.getUserName().equals(sellingUser)) result.add(tr.getItemName());
                }
            }
        }
        return result;
    }

    Graph buildGraph(WantList wantLists, Set<String> errors) {
        Graph graph = new Graph();
        Map<Double, List<TradeRequest>> moneyAmounts = new HashMap<Double, List<TradeRequest>>();
        HashMap< String,Integer > unknowns = new HashMap<String,Integer>();
        // create the nodes
        for (TradeRequest request : wantLists.getTradeRequests()) {
            String user = request.getUserName();
            String name = request.getItemName();
            boolean isDummy = request.isDummy();
            boolean isMoney = request.isMoney();
            Vertex vertex = graph.addVertex(name, user, isDummy, isMoney, request);
            if (!isDummy) width = Math.max(width, show(vertex).length());
            if (isMoney) {
                double amount = request.getAmount();
                List<TradeRequest> forThisAmount = moneyAmounts.get(amount);
                if (forThisAmount == null) {
                    forThisAmount = new ArrayList<TradeRequest>();
                    moneyAmounts.put(amount, forThisAmount);
                }
                forThisAmount.add(request);
            }
        }

        // create the edges
        for (TradeRequest request : wantLists.getTradeRequests()) {
            String fromName = request.getItemName();
            Vertex fromVertex = graph.getVertex(fromName);

            // add the "no-trade" edge to itself
            graph.addEdge(fromVertex,fromVertex.twin,nonTradeCost, request);

            long rank = 1;
            List<String> listWithoutMoney = new ArrayList<String>();
            for (String toName : request.willAccept()) {
                if (toName.startsWith("$")) {
                    listWithoutMoney.addAll(findSuitableMoneyBids(Money.parseMoney(toName), request.getUserName(), moneyAmounts));
                } else {
                    listWithoutMoney.add(toName);
                }
            }
            for (String toName : listWithoutMoney) {
                if (toName.equals(";")) {
                    rank += bigStep;
                    continue;
                }
                if (toName.charAt(0) == '%') {
                    if (fromVertex.user == null) {
                        errors.add("**** Dummy item " + toName + " used in want list for item " + fromName + ", which does not have a username.");
                        continue;
                    }
                    toName += " for " + fromVertex.user;
                }
                Vertex toVertex = graph.getVertex(toName);
                if (toVertex == null) {
                    int occurrences = unknowns.containsKey(toName) ? unknowns.get(toName) : 0;
                    unknowns.put(toName,occurrences + 1);
                    continue;
                }

                toVertex = toVertex.twin; // adjust to the sending vertex
                if (toVertex == fromVertex.twin) {
                    errors.add(fromVertex.user + ": Item " + toName + " appears in its own want list.");
                } else if (graph.getEdge(fromVertex,toVertex) != null) {
                    if (showRepeats) {
                        errors.add(fromVertex.user + ": Item " + toName + " is repeated in want list");
                    }
                } else if (!toVertex.isDummy &&
                        fromVertex.user != null &&
                        fromVertex.user.equals(toVertex.user)) {
                    errors.add(fromVertex.user + ": Item "+fromVertex.name +" contains item "+toVertex.name+" from the same user");
                } else {
                    long cost = UNIT;
                    switch (priorityScheme) {
                        case LINEAR_PRIORITIES:   cost = rank; break;
                        case TRIANGLE_PRIORITIES: cost = rank*(rank+1)/2; break;
                        case SQUARE_PRIORITIES:   cost = rank*rank; break;
                        case SCALED_PRIORITIES:   cost = rank; break; // assign later
                        case EXPLICIT_PRIORITIES: cost = rank; break;
                    }

                    // all edges out of a dummy node have the same cost
                    if (fromVertex.isDummy) cost = nonTradeCost;

                    graph.addEdge(fromVertex,toVertex,cost, request);

                    rank += smallStep;
                }
            }

            // update costs for those priority schemes that need information such as
            // number of wants
            if (!fromVertex.isDummy) {
                switch (priorityScheme) {
                    case SCALED_PRIORITIES:
                        int n = fromVertex.edges.size()-1;
                        for (Edge edge : fromVertex.edges) {
                            if (edge.sender != fromVertex.twin)
                                edge.cost = 1 + (edge.cost-1)*2520/n;
                        }
                        break;
                }
            }
        }

        graph.freeze();
        return graph;
    }

    String show(Vertex vertex) {
        if (vertex.user == null || vertex.isDummy) return vertex.name;
        return vertex.summary();
//        else if (sortByItem) return vertex.name + " " + vertex.user;
//        else return vertex.user + " " + vertex.name;
    }

    String show(TradeRequest request) {
        return request.getItemSummary();
    }

    /**
     * User sender sends Item item to User receiver. Add corresponding entries to the sends and receives maps.
     */
    private void addTrade(Map<String, List<String>> sends, Map<String, List<String>> receives, String sender, String receiver, String item) {
        String s = item + " to " + receiver;
        List<String> ss = sends.get(sender);
        if (ss == null) ss = new ArrayList<String>();
        ss.add(s);
        sends.put(sender, ss);
        s = item + " from " + sender;
        List<String> rr = receives.get(receiver);
        if (rr == null) rr = new ArrayList<String>();
        rr.add(s);
        receives.put(receiver, rr);
    }

    private void addNoTrade(Map<String, List<String>> noTrades, String user, String item, List<String> wantedBy) {
        List<String> ss = noTrades.get(user);
        if (ss == null) ss = new ArrayList<String>();
        String entry = item;
        if (wantedBy.size() > 0) {
            entry = entry + " (wanted by ";
            for (int i=0; i<wantedBy.size(); i++) {
                if (i > 0) entry = entry + ", ";
                entry = entry + wantedBy.get(i);
            }
            entry = entry + ")";
        }
        ss.add(entry);        
        noTrades.put(user, ss);
    }

    private void addExchange(Map<String, List<String>> exchanges, String user, String game1, String game2) {
        List<String> es = exchanges.get(user);
        if (es == null) es = new ArrayList<String>();
        es.add("receives " + game1 + " in exchange for " + game2);
        exchanges.put(user, es);
    }

    void displayMatchesHtml(Solution solution, Geeklist list, WantList wants, File dest) throws Exception {
        FileWriter f = new FileWriter(dest);
        PrintWriter pw = new PrintWriter(f);
        pw.println("<HTML><HEADER><meta charset=\"UTF-8\"><BODY>");
        Map<String, List<String>> sends = new HashMap<String, List<String>>();
        Map<String, List<String>> receives = new HashMap<String, List<String>>();
        Map<String, List<String>> noTrades = new HashMap<String, List<String>>();
        Map<String, List<String>> exchanges = new HashMap<String, List<String>>();
        List<Cycle> cycles = solution.getCycles();
        Graph graph = solution.getGraph();
        for (Cycle cycle : cycles) {
            for (Vertex v : cycle.getVertices()) {
                assert v.match != v.twin;
                String user1 = v.getTradeRequest().getUserName();
                String user2 = v.match.getTradeRequest().getUserName();
                String name1, name2;
                GeeklistItem i1 = list.getItemForVertex(v);
                if (i1 == null) {
                    name1 = v.summary();
                } else {
                    name1 = i1.getItemHtml();
                }
                GeeklistItem i2 = list.getItemForVertex(v.match);
                if (i2 == null) {
                    name2 = v.match.summary();
                } else {
                    name2 = i2.getItemHtml();
                }
                addTrade(sends, receives, user2, user1, name2);
                addExchange(exchanges, user1, name2, name1);
            }
        }
        for (Vertex v : graph.RECEIVERS) {
            if (v.match == v.twin && !v.isDummy && !v.isMoney) {
                GeeklistItem item = list.getItemForVertex(v);
                addNoTrade(noTrades, item.getUserName(), item.getNameHtml(), wants.getWantedBy(item.getTradeCode()));
            }
        }
        for (Vertex v : graph.orphans) {
            if (!v.isDummy && !v.isMoney) {
                GeeklistItem item = list.getItemForVertex(v);
                if (item == null) {
                    System.out.println("v = " + v);
                } else {
                    addNoTrade(noTrades, item.getUserName(), item.getNameHtml(), wants.getWantedBy(item.getTradeCode()));
                }
            }
        }
        List<String> users = list.getUsers();
        for (String u : users) {
            pw.println("<A NAME=\"" + u + "\"><H2>" + u + "</H2></A>");
            List<String> us = sends.get(u);
            if (us == null) {
                pw.println(u + " does not send anything.<BR>");
            } else {
                pw.println(u + " sends:<ul>");
                for (String s : us) {
                    pw.println("<LI>" + s + "<BR>");
                }
                pw.println("</ul><br>");
            }
            List<String> ur = receives.get(u);
            if (ur == null) {
                pw.println(u + " does not receive anything.<BR>");
            } else {
                pw.println(u + " receives:<ul>");
                for (String s : ur) {
                    pw.println("<LI>" + s + "<BR>");
                }
                pw.println("</ul><br>");
            }
            List<String> un = noTrades.get(u);
            if (un != null) {
                pw.println("These games do not trade:<ul>");
                for (String s : un) {
                    String extra = "";
                    if (s.indexOf("Mouse Trap") >= 0) extra = " and you have to send $30 to Friendless.";
                    pw.println("<LI>" + s + extra + "<BR>");
                }
                pw.println("</ul><br>");
            }
            List<String> ue = exchanges.get(u);
            if (ue != null) {
                pw.println(u + ":<BR><ul>");
                for (String s : ue) {
                    pw.println("<LI>" + s + "<BR>");
                }
                pw.println("</ul><br>");
            }
        }

        pw.println("</BODY></HTML>");
        pw.close();
        f.close();
    }

    void displayMatches(Solution solution, PrintStream out) {
        List<Cycle> cycles = solution.getCycles();
        Graph graph = solution.getGraph();
        int numTrades = 0;
        int numGames = 0;
        int numGroups = cycles.size();
        int sumOfSquares = 0;
        List< Integer > groupSizes = new ArrayList< Integer >();

        List< String > summary = new ArrayList< String >();
        List< String > loops = new ArrayList< String >();

        for (Cycle cycle : cycles) {
            int size = cycle.size();
            numTrades += size;
            sumOfSquares += size*size;
            groupSizes.add(size);
            for (Vertex v : cycle.getVertices()) {
                assert v.match != v.twin;
                if (!v.match.isMoney) numGames++;
                loops.add(pad(show(v)) + " receives " + show(v.match.twin));
                summary.add(pad(show(v)) + " receives " + pad(show(v.match.twin)) + " and sends to " + show(v.twin.match));
            }
            loops.add("");
        }
        if (showNonTrades) {
            for (Vertex v : graph.RECEIVERS) {
                if (v.match == v.twin && !v.isDummy && !v.isMoney)
                    summary.add(pad(show(v)) + "             does not trade");
            }
            for (Vertex v : graph.orphans) {
                if (!v.isDummy && !v.isMoney)
                    summary.add(pad(show(v)) + "             does not trade");
            }
        }

        out.println("TRADE LOOPS (" + numTrades + " total trades):");
        out.println();
        for (String item : loops) out.println(item);

        Collections.sort(summary);
        out.println("ITEM SUMMARY (" + numTrades + " total trades):");
        out.println();
        for (String item : summary) out.println(item);
        System.out.println();

        System.out.println("Num trades  = " + numTrades + " (" + numGames + " games)");
        System.out.println("Num groups  = " + numGroups);
        System.out.print("Group sizes =");
        Collections.sort(groupSizes);
        Collections.reverse(groupSizes);
        for (int groupSize : groupSizes) System.out.print(" " + groupSize);
        System.out.println();
        System.out.println("Sum squares = " + sumOfSquares);
    }

    int width = 1;
    String pad(String name) {
        while (name.length() < width) name += " ";
        return name;
    }
}
