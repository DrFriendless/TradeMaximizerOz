/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 04/12/2010
 * Time: 8:53:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class Money {
    static int parseMoney(String s) {
        if (!s.startsWith("$")) throw new RuntimeException(s);
        double d = Integer.parseInt(s.substring(1));
        if (d < 0 || d > 3000) throw new RuntimeException(s);
//        if (!s.equals("$" + d)) throw new RuntimeException(s);
        return (new Double(d)).intValue();
    }
}
