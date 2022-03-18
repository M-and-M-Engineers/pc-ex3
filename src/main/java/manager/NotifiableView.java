package manager;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;
import scm.State;

public class NotifiableView extends VerticalLayout {

    private static final int DURATION = 5000;
    private final DashBoardService service;
    private final UI ui;

    public NotifiableView() {
        this.service = VaadinSession.getCurrent().getAttribute(DashBoardService.class);
        this.ui = UI.getCurrent();

        if (!this.service.areNotificationsActive()) {
            this.service.getNames().forEach(this::subscribeToUpdates);
            this.service.activateNotifications();
        }
    }

    private void subscribeToUpdates(final int port, final String name) {
        this.service.subscribeToServed(port, json -> {
            final String product = json.getString("product");
            final int remaining = json.getInteger("remaining");
            final int sugarRemaining = json.getInteger("sugarRemaining");
            final int glassesRemaining = json.getInteger("glassesRemaining");

            if (remaining == 0)
                this.showNotification("Product '" + product + "' of coffe machine " + name + " is out of stock.");
            else if (remaining < 3)
                this.showNotification("Coffee machine " + name + " is running out of product '" + product + "'. There are only " + remaining + " units left.");

            if (sugarRemaining == 0)
                this.showNotification("'Sugar' of coffe machine " + name + " is out of stock.");
            else if (sugarRemaining < 15)
                this.showNotification("Coffee machine " + name + " is running out of 'Sugar'. There are only " + sugarRemaining + " units left.");

            if (glassesRemaining == 0)
                this.showNotification("'Glasses' of coffe machine " + name + " is out of stock.");
            else if (glassesRemaining < 7)
                this.showNotification("Coffee machine " + name + " is running out of 'Glasses'. There are only " + glassesRemaining + " units left.");
        });

        this.service.subscribeToStateChanged(port,
                s -> this.showNotification("Smart Coffee Machine " + name + " is now " + s),
                unused -> this.showNotification("Smart Coffee Machine " + name + " is " + State.OUT_OF_SERVICE));
    }

    protected void updateUI(Command command) {
        this.ui.access(command);
    }

    protected void showNotification(final String text) {
        this.updateUI(() -> Notification.show(text, DURATION, Notification.Position.MIDDLE));
    }
}

