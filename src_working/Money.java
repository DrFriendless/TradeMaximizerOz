/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 04/12/2010
 * Time: 8:53:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class Money {
    static double parseMoney(String s) {
        if (!s.startsWith("$")) throw new RuntimeException(s);
        double d;
        try {
            d = Double.parseDouble(s.substring(1));
        } catch (NumberFormatException ex) {
            d = Integer.parseInt(s.substring(1));
        }
        if (d < 0.0 || d > 3000.0) throw new RuntimeException(s);
//        if (!s.equals("$" + d)) throw new RuntimeException(s);
        return d;
    }
}
