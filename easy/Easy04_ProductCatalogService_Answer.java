/**
 * ANSWER FILE — Easy04_ProductCatalogService
 * ===========================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — NPE: cheapest Is null on the First Iteration (Line 23)
 * ─────────────────────────────────────────────────────────
 * What  : cheapest starts as null. On the very first iteration,
 *         cheapest.getPrice() throws NullPointerException before a single
 *         comparison is made.
 * Why   : The guard condition itself contains the NPE — even an empty-list
 *         case is not handled. This crashes on every non-null call.
 * Fix   : Initialize cheapest to products.get(0) and loop from index 1,
 *         or use Optional / a null guard inside the loop.
 *
 * Review comment:
 *   "cheapest is initialized to null. On the first iteration cheapest.getPrice()
 *    throws NPE before any comparison happens. Initialize cheapest to
 *    products.get(0) and start the loop at index 1, or add a null guard."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — String Compared with == Instead of .equals() (Line 34)
 * ─────────────────────────────────────────────────────────
 * What  : product.getCategory() == FEATURED compares object references,
 *         not string content. Category strings returned by a service or
 *         database are never interned.
 * Why   : This will silently return false for every real product — even
 *         one whose category is genuinely "FEATURED" — because the string
 *         objects are different instances.
 * Fix   : Use FEATURED.equals(product.getCategory()) — literal on the left
 *         guards against NPE if getCategory() returns null.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — Floating-Point Precision for Currency (Lines 40-42)
 * ─────────────────────────────────────────────────────────
 * What  : (int)(1.05 * 100) evaluates to 104, not 105, due to IEEE 754
 *         binary representation. formatPrice(1.05) returns "$1.4".
 * Why   : double cannot represent many decimal fractions exactly. Currency
 *         arithmetic using double silently produces wrong results.
 * Fix   : Use BigDecimal: new BigDecimal(String.valueOf(price)) to avoid
 *         floating-point rounding, then format with DecimalFormat or
 *         String.format("$%.2f", price).
 *
 * Review comment:
 *   "(int)(price * 100) loses precision — 1.05 * 100 = 104.9999... in IEEE 754.
 *    formatPrice(1.05) returns $1.4 instead of $1.05. Use BigDecimal or
 *    String.format(\"$%.2f\", price) for currency formatting."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 4 — String Concatenation in Loop — O(n²) (Lines 49-51)
 * ─────────────────────────────────────────────────────────
 * What  : out = out + ... creates a new String on every iteration, copying
 *         all prior content. For a catalogue with 10,000 products this is
 *         O(n²) work.
 * Fix   : Use StringBuilder or String.join() for O(n) performance.
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE
 * ─────────────────────────────────────────────────────────
 */
import java.math.BigDecimal;
import java.util.*;

public class Easy04_ProductCatalogService_Answer {

    private static final String FEATURED = "FEATURED";

    // Fix 1: seed with first element, loop from 1; also guard empty list
    public Product findCheapest(List<Product> products) {
        if (products == null || products.isEmpty()) return null;
        Product cheapest = products.get(0);
        for (int i = 1; i < products.size(); i++) {
            if (products.get(i).getPrice() < cheapest.getPrice()) {
                cheapest = products.get(i);
            }
        }
        return cheapest;
    }

    // Fix 2: literal on left → safe if getCategory() returns null
    public boolean isFeatured(Product product) {
        return FEATURED.equals(product.getCategory());
    }

    // Fix 3: String.format avoids floating-point precision issues
    public String formatPrice(double price) {
        return String.format("$%.2f", price);
    }

    // Fix 4: StringBuilder → O(n)
    public String buildCatalogDump(List<Product> products) {
        StringBuilder sb = new StringBuilder();
        for (Product p : products) {
            sb.append(p.getName()).append(": $").append(p.getPrice()).append('\n');
        }
        return sb.toString();
    }
}
