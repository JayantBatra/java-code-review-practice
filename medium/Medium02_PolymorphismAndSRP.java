/**
 * DIFFICULTY : Medium
 * SNIPPET    : 02 — Polymorphism & Single Responsibility Principle
 *
 * Instructions
 * ------------
 * Review the code below as if you were doing a professional code review.
 * Find ALL issues, explain WHY each is a problem, and describe the fix.
 * Try to spot every issue before looking at Medium02_PolymorphismAndSRP_Answer.java.
 *
 * Hint: there are 3 main design issues (one has two sub-problems).
 */
public class Medium02_PolymorphismAndSRP {

    /**
     * Handles notifications for different channel types.
     * New channels (PUSH, WHATSAPP) are expected to be added in the future.
     */
    static class NotificationService {

        public void send(Notification notification) {
            if (notification.getChannel().equals("EMAIL")) {
                String subject = notification.getSubject();
                String body    = notification.getBody();
                String to      = notification.getRecipient();
                emailClient.connect();
                emailClient.authenticate();
                emailClient.sendMail(to, subject, body);
                emailClient.disconnect();

            } else if (notification.getChannel().equals("SMS")) {
                String phone   = notification.getPhone();
                String message = notification.getBody();
                smsGateway.dial(phone);
                smsGateway.transmit(message);

            } else if (notification.getChannel().equals("SLACK")) {
                String channel = notification.getSlackChannel();
                String text    = notification.getBody();
                slackClient.post(channel, text);
            }
        }

        public String buildReceipt(Notification notification) {
            if (notification.getChannel().equals("EMAIL")) {
                return "Email sent to " + notification.getRecipient();

            } else if (notification.getChannel().equals("SMS")) {
                return "SMS sent to " + notification.getPhone();

            } else if (notification.getChannel().equals("SLACK")) {
                return "Slack message posted to " + notification.getSlackChannel();
            }
            return "Unknown";
        }

        public boolean requiresDeliveryConfirmation(Notification notification) {
            if (notification.getChannel().equals("EMAIL")) {
                return true;
            } else if (notification.getChannel().equals("SMS")) {
                return true;
            } else if (notification.getChannel().equals("SLACK")) {
                return false;
            }
            return false;
        }
    }

    /**
     * Processes an order end-to-end.
     */
    static class OrderProcessor {

        public void process(Order order) {
            // Step 1: validate
            if (order == null)                       throw new IllegalArgumentException("null order");
            if (order.getItems() == null || order.getItems().isEmpty()) throw new ValidationException("empty");
            if (order.getUser() == null)             throw new ValidationException("no user");

            // Step 2: apply discount
            double total = 0;
            for (Item item : order.getItems()) {
                total += item.getPrice() * item.getQuantity();
            }
            if (order.getPromoCode() != null) {
                PromoCode promo = promoService.validate(order.getPromoCode());
                total = total * (1 - promo.getDiscountRate());
            }

            // Step 3: charge
            paymentGateway.charge(order.getUser().getPaymentMethod(), total);

            // Step 4: reduce stock
            for (Item item : order.getItems()) {
                inventoryService.reduce(item.getProductId(), item.getQuantity());
            }

            // Step 5: notify
            String msg = "Your order " + order.getId() + " is confirmed. Total: " + total;
            notificationService.send(order.getUser().getEmail(), msg);

            // Step 6: audit
            auditLog.record("ORDER_PLACED", order.getId(), order.getUser().getId());
        }
    }
}
