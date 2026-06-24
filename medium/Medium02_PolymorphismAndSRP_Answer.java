/**
 * ANSWER FILE — Medium02_PolymorphismAndSRP
 * ==========================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — Polymorphism Opportunity: Repeated instanceof / type-string Chains
 * ─────────────────────────────────────────────────────────
 * What  : The same `notification.getChannel().equals("EMAIL/SMS/SLACK")` chain
 *         appears in THREE separate methods: send(), buildReceipt(), and
 *         requiresDeliveryConfirmation(). There are also channel-specific
 *         fields mixed into the Notification class.
 * Why   : Every time a new channel is added (PUSH, WHATSAPP), a developer
 *         must find and update ALL three methods. Missing even one creates a
 *         silent bug. This is an Open/Closed Principle violation — the class
 *         is not closed for modification.
 *         This is the clearest possible signal that polymorphism is the fix.
 * Fix   : Introduce a NotificationChannel interface with send(), buildReceipt(),
 *         and requiresDeliveryConfirmation(). Create EmailChannel, SmsChannel,
 *         SlackChannel implementations. NotificationService delegates to the
 *         interface — it never checks the type again.
 *
 * Review comment:
 *   "The same channel type-check appears in send(), buildReceipt(), and
 *    requiresDeliveryConfirmation(). Adding a new channel (e.g. PUSH) requires
 *    updating all three — easy to miss. This is a textbook polymorphism
 *    opportunity: introduce a NotificationChannel interface, move each channel's
 *    logic into its own class, and let send() just call channel.send()."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — Magic Strings for Channel Types
 * ─────────────────────────────────────────────────────────
 * What  : "EMAIL", "SMS", "SLACK" are raw string literals scattered across
 *         three methods.
 * Why   : A typo ("EMAlL") silently falls through to the default case with no
 *         compile-time error. Adding a new channel requires a grep across the
 *         codebase to find every occurrence.
 * Fix   : Use an enum ChannelType { EMAIL, SMS, SLACK } — then the compiler
 *         enforces exhaustiveness (especially useful in switch expressions).
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — OrderProcessor.process() Violates SRP (Has 6 Responsibilities)
 * ─────────────────────────────────────────────────────────
 * What  : A single method handles: (1) validation, (2) pricing/discount,
 *         (3) payment, (4) inventory, (5) notification, (6) auditing.
 * Why   : - Unit testing is nearly impossible without mocking all 6 dependencies.
 *         - Any change to pricing logic sits next to payment logic — risk of
 *           accidental side-effects.
 *         - The method will grow without bound as requirements expand.
 *         Each of these 6 concerns has a different reason to change.
 * Fix   : Extract each step into a private method (or collaborating service).
 *         process() becomes an orchestrator of single-purpose calls.
 *
 * Review comment:
 *   "process() handles validation, pricing, payment, inventory, notification,
 *    and auditing in one 30-line block. Each has a different reason to change,
 *    and unit testing any one step requires standing up all six dependencies.
 *    Suggest extracting into focused private methods: validateOrder(),
 *    calculateTotal(), chargePayment(), updateInventory(), notifyUser(),
 *    recordAudit()."
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE (structure — not compiling without full context)
 * ─────────────────────────────────────────────────────────
 */
public class Medium02_PolymorphismAndSRP_Answer {

    // Fix 1 & 2: interface replaces all type checks; enum replaces magic strings
    enum ChannelType { EMAIL, SMS, SLACK }

    interface NotificationChannel {
        void send(Notification n);
        String buildReceipt(Notification n);
        boolean requiresDeliveryConfirmation();
    }

    static class EmailChannel implements NotificationChannel {
        @Override public void send(Notification n) {
            emailClient.connect();
            emailClient.authenticate();
            emailClient.sendMail(n.getRecipient(), n.getSubject(), n.getBody());
            emailClient.disconnect();
        }
        @Override public String buildReceipt(Notification n) { return "Email sent to " + n.getRecipient(); }
        @Override public boolean requiresDeliveryConfirmation() { return true; }
    }

    static class SmsChannel implements NotificationChannel {
        @Override public void send(Notification n) { smsGateway.dial(n.getPhone()); smsGateway.transmit(n.getBody()); }
        @Override public String buildReceipt(Notification n) { return "SMS sent to " + n.getPhone(); }
        @Override public boolean requiresDeliveryConfirmation() { return true; }
    }

    static class SlackChannel implements NotificationChannel {
        @Override public void send(Notification n) { slackClient.post(n.getSlackChannel(), n.getBody()); }
        @Override public String buildReceipt(Notification n) { return "Slack posted to " + n.getSlackChannel(); }
        @Override public boolean requiresDeliveryConfirmation() { return false; }
    }

    // NotificationService is now trivially simple and never needs to change for new channels
    static class NotificationService {
        private final Map<ChannelType, NotificationChannel> channels = Map.of(
            ChannelType.EMAIL, new EmailChannel(),
            ChannelType.SMS,   new SmsChannel(),
            ChannelType.SLACK, new SlackChannel()
        );

        public void send(Notification n) {
            channels.get(n.getChannelType()).send(n);
        }
    }

    // Fix 3: orchestrator only — each step has one reason to change
    static class OrderProcessor {
        public void process(Order order) {
            validateOrder(order);
            double total = calculateTotal(order);
            chargePayment(order, total);
            updateInventory(order);
            notifyUser(order, total);
            recordAudit(order);
        }

        private void validateOrder(Order order) { /* ... */ }
        private double calculateTotal(Order order) { /* ... */ return 0; }
        private void chargePayment(Order order, double total) { /* ... */ }
        private void updateInventory(Order order) { /* ... */ }
        private void notifyUser(Order order, double total) { /* ... */ }
        private void recordAudit(Order order) { /* ... */ }
    }
}
