package manager;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;
import scm.Status;

public class NotifiableView extends VerticalLayout {

    private static final int DURATION = 5000;
    private final UI ui;

    public NotifiableView() {
        final DashBoardService service = VaadinSession.getCurrent().getAttribute(DashBoardService.class);
        this.ui = UI.getCurrent();

        service.getDigitalTwinNames().forEach(scm -> {

            service.subscribeToServed(scm, json -> {
                final String product = json.getString("product");
                final int remaining = json.getInteger("remaining");
                final int sugarRemaining = json.getInteger("sugarRemaining");
                final int glassesRemaining = json.getInteger("glassesRemaining");

                if (remaining < 3)
                    this.showNotification("Product " + product + " of coffee machine " + scm + " is nearly over. There are only " + remaining + " units left.");

                if (sugarRemaining < 15)
                    this.showNotification("Sugar of coffee machine " + scm + " is nearly over. There are only " + sugarRemaining + " units left.");

                if (glassesRemaining < 7)
                    this.showNotification("Glasses of coffee machine " + scm + " are nearly over. There are only " + glassesRemaining + " units left.");
            });

            service.subscribeToStatusChanged(scm,
                    s -> this.showNotification("Smart Coffee Machine " + scm + " is now " + s),
                    unused -> this.showNotification("Smart Coffee Machine " + scm + " is " + Status.OUT_OF_SERVICE));

        });
    }

    protected void updateUI(Command command) {
        this.ui.access(command);
    }

    protected void showNotification(final String text) {
        this.updateUI(() -> Notification.show(text, DURATION, Notification.Position.MIDDLE));
    }
}

