/**
 * DIFFICULTY : Easy
 * SNIPPET    : 04 — Product Catalog Service
 *
 * Instructions
 * ------------
 * Review the code below as if you were doing a professional code review.
 * Find ALL issues, explain WHY each is a problem, and describe the fix.
 * Try to spot every issue before looking at Easy04_ProductCatalogService_Answer.java.
 *
 * Hint: there are 4 distinct issues hidden in this class.
 */
import java.util.*;

public class Easy04_ProductCatalogService {

    private static final String FEATURED = "FEATURED";

    /**
     * Finds the cheapest product in the list.
     */
    public Product findCheapest(List<Product> products) {
        Product cheapest = null;
        for (Product p : products) {
            if (p.getPrice() < cheapest.getPrice()) {
                cheapest = p;
            }
        }
        return cheapest;
    }

    /**
     * Returns true if the product is in the featured category.
     */
    public boolean isFeatured(Product product) {
        return product.getCategory() == FEATURED;
    }

    /**
     * Formats a price as a dollar string, e.g. 1.05 → "$1.05".
     */
    public String formatPrice(double price) {
        int cents = (int) (price * 100);
        return "$" + cents / 100 + "." + cents % 100;
    }

    /**
     * Builds a newline-separated dump of all products for logging.
     */
    public String buildCatalogDump(List<Product> products) {
        String out = "";
        for (Product p : products) {
            out = out + p.getName() + ": $" + p.getPrice() + "\n";
        }
        return out;
    }
}
