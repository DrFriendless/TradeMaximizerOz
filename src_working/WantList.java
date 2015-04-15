import java.io.PrintStream;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 11/12/2010
 * Time: 12:17:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class WantList {
    private List<TradeRequest> wants;
    private Geeklist geeklist;
    private Map<String, Double> spendingLimits = new HashMap<String, Double>();
    private Set<String> errors;
    private Map<String, Set<String>> wantedBy = new HashMap<String, Set<String>>();

    WantList(List<TradeRequest> wants, Set<String> errors) {
        this.wants = wants;
        this.errors = errors;
        inferSpendingLimits();
    }

    private void inferSpendingLimits() {
        Map<String, Double> highestOffer = new HashMap<String, Double>();
        Map<String, Integer> numOffers = new HashMap<String, Integer>();
        for (Iterator<TradeRequest> iter = wants.iterator(); iter.hasNext(); ) {
            TradeRequest request = iter.next();
            if (request.getItemName().equalsIgnoreCase("LIMIT")) {
                if (spendingLimits.containsKey(request.getUserName())) {
                    errors.add(request.getUserName() + " has more than one spending limit");
                }
                if (request.willAccept().size() > 1) {
                    throw new RuntimeException(request.getUserName() + " has a nonsense spending limit");
                }
                spendingLimits.put(request.getUserName(), Money.parseMoney(request.willAccept().get(0)));
                iter.remove();
            } else if (request.isMoney()) {
                double m = request.getAmount();
                if (highestOffer.get(request.getUserName()) == null || m > highestOffer.get(request.getUserName())) {
                    highestOffer.put(request.getUserName(), m);
                }
                Integer n = numOffers.get(request.getUserName());
                if (n == null) n = 0;
                n = n + 1;
                numOffers.put(request.getUserName(), n);
            }
        }
        for (Map.Entry<String, Double> entry : highestOffer.entrySet()) {
            if (!spendingLimits.containsKey(entry.getKey()) && numOffers.get(entry.getKey()) > 1) {
                spendingLimits.put(entry.getKey(), entry.getValue());
                errors.add(entry.getKey() + " is using a default spending limit");
            }
        }
    }

    public void setGeekList(Geeklist geeklist, Set<String> errors) {
        this.geeklist = geeklist;
        List<String> users = geeklist.getAllUsers();
        for (TradeRequest tr : wants) {
            if (!users.contains(tr.getUserName())) {
                // check for people using the wrong case in their user name in their wants.
                // this happens all the time.
                boolean found = false;
                for (String u : users)  {
                    if (u.equalsIgnoreCase(tr.getUserName())) {
                        tr.setUserName(u);
                        found = true;
                        break;
                    }
                }
                if (!found) errors.add("User " + tr.getUserName() + " has submitted wants but is not in the geeklist.");
            }
        }
    }

    public void check(Set<String> errors, PrintStream out) {
        Set<String> names = new HashSet<String>();
        for (TradeRequest tr : wants) {
            if (names.contains(tr.getItemName())) {
                errors.add(tr.getUserName() + "has a duplicate entry for " + tr.getItemName());
            }
            names.add(tr.getItemName());
        }
        Map<String, Integer> numWant = calcMostWanted(errors, out);
        // UNWANTED
        List<TradeRequest> unwanted = new ArrayList<TradeRequest>();
        for (TradeRequest tr : wants) {
            if (!numWant.keySet().contains(tr.getItemName()) && !tr.isDummy() && !tr.isMoney()) unwanted.add(tr);
        }
        Collections.sort(unwanted);
        out.println();
        out.println("UNWANTED");
        for (TradeRequest s : unwanted) {
            GeeklistItem item = geeklist.getItem(s.getItemName());
            if (item == null) {
                out.println("s = " + s.getItemName());
            } else {
                out.println(s.getItemName() + " " + item.getName());
            }
        }
    }

    private Map<String, Integer> calcMostWanted(Set<String> errors, PrintStream out) {
        Map<String, List<String>> dummyIndex = new HashMap<String, List<String>>();
        for (TradeRequest wantList : wants) {
            if (wantList.isDummy()) {
                dummyIndex.put(wantList.getItemName(), new ArrayList<String>(wantList.willAccept()));
            }
        }
        Map<String, Integer> numWant = new HashMap<String, Integer>();
        for (TradeRequest wantList : wants) {
            String user = wantList.getUserName();
            String userGame = wantList.getItemName();
            if (wantList.isDummy()) {
                // dummy item                
            } else if (wantList.isMoney()) {
                // money
            } else {
                GeeklistItem userItem = checkGameExists(userGame, wantList, geeklist, errors);
                if (userItem != null && !userItem.getUserName().equalsIgnoreCase(user.replace('#', ' '))) {
                    errors.add(user + " submitted wants for an item which is not his: " + wantList);
                }
            }
            List<String> temp = new ArrayList<String>(wantList.willAccept());
            Set<String> willAccept = new HashSet<String>();
            for (String s : temp) {
                if (s.startsWith("$")) continue;
                if (s.startsWith("%")) {
                    String key = s + " for " + user;
                    List<String> lookup = dummyIndex.get(key);
                    if (lookup != null) willAccept.addAll(lookup);
                } else {
                    willAccept.add(s);
                }
            }
            for (String name : willAccept) {
                checkGameExists(name, wantList, geeklist, errors);
                Integer c = numWant.get(name);
                if (c == null) c = 0;
                c = c + 1;
                numWant.put(name, c);
                if (!name.startsWith("$") && !name.startsWith("%")) {
                    Set<String> wb = wantedBy.get(name);
                    if (wb == null) wb = new HashSet<String>();
                    wb.add(user);
                    if (wantList.isMoney()) wb.add("$" + Math.round(wantList.getAmount()));
                    wantedBy.put(name, wb);
                }
            }
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(numWant.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return -o1.getValue().compareTo(o2.getValue());
            }
        });
        out.println();
        out.println("MOST WANTED");
        for (Map.Entry<String, Integer> e : entries) {
            GeeklistItem item = geeklist.getItem(e.getKey());
            if (item == null) continue;
            out.println(item.getTradeCode() + ": " + item.getName() + " wanted in exchange for " + e.getValue() + " items");
        }
        out.println();
        return numWant;
    }

    private GeeklistItem checkGameExists(String code, TradeRequest wants, Geeklist geeklist, Set<String> errorLines) {
        if (code.startsWith("$") || code.startsWith("%")) return null;
        if (geeklist == null) return null;
        GeeklistItem item = geeklist.getItem(code);
        if (item == null) {
            errorLines.add(wants.getUserName() + ": Unknown code " + code);
        }
        return item;
    }

    void showUnsubmitted() {
        List<String> allCodes = geeklist.getAllCodes();
        List<String> allUsers = geeklist.getAllUsers();
        for (TradeRequest want : wants) {
            String itemName = want.getItemName();
            if (itemName.startsWith("%")) continue;
            if (itemName.startsWith("$")) continue;
            GeeklistItem item = geeklist.getItem(itemName);
            if (item == null) {
                errors.add("WHAT IS item = " + itemName);
                continue;
            }
            allCodes.remove(item.getTradeCode());
            allUsers.remove(item.getUserName());
        }
        for (String u : allUsers) {
            errors.add(u + " has not submitted a wants list");
        }
        for (String code : allCodes) {
            GeeklistItem item = geeklist.getItem(code);
            errors.add("(" + item.getUserName() + ") " + item.getTradeCode() + " : ");
        }
    }

    List<TradeRequest> getTradeRequests() {
        return new ArrayList<TradeRequest>(wants);
    }

    /**
     * Check the spending limits. If they've been exceeded, return the name of the person who did it by most.
     * @param paid
     * @return
     */
    public String satisfiesSpendingLimits(Map<String, Double> paid) {
        List<String> users = new ArrayList<String>();
        double most = 0;
        for (Map.Entry<String, Double>  entry : paid.entrySet()) {
            Double limit = spendingLimits.get(entry.getKey());
            if (limit == null) limit = 0.0;
            if (entry.getValue() - limit > most) {
                users.clear();
                most = entry.getValue() - limit;
            }
            if (entry.getValue() - limit == most && most > 0) {
                users.add(entry.getKey());
            }
        }
        Collections.sort(users);
        if (users.size() > 0) {
            System.out.println(users.get(0) + " exceeded spending limit by " + most);
            return users.get(0);
        }
        return null;
    }

    public void removeRequest(TradeRequest request) {
        wants.remove(request);
    }

    List<String> getWantedBy(String code) {
        Set<String> s = wantedBy.get(code);
        if (s == null) return Collections.emptyList();
        List<String> l = new ArrayList<String>(s);
        Collections.sort(l);
        return l;
    }
}
