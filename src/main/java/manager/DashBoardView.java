package manager;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import scm.Status;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

@Route("/")
@PageTitle("DashBoard")
//@CssImport("./styles/shared-styles.css")
//@CssImport(value = "./styles/vaadin-text-field-styles.css", themeFor = "vaadin-text-field")
public class DashBoardView extends NotifiableView {

    private static final String STATUS_TEXT = "Status -> ";
    private final DashBoardService service;
    private final CountDownLatch latch;

    public DashBoardView() {
        super();
        this.service = VaadinSession.getCurrent().getAttribute(DashBoardService.class);

        final H1 heading = new H1("Manager DashBoard");
        add(heading);

        final Set<String> digitalTwinNames = this.service.getDigitalTwinNames();
        this.latch = new CountDownLatch(digitalTwinNames.size());
        digitalTwinNames.forEach(this::addNewElement);
        try {
            this.latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        setAlignItems(Alignment.CENTER);
    }

    private void addNewElement(final String name) {
        final HorizontalLayout layout = new HorizontalLayout();

        final RouterLink link = new RouterLink(name, SmartCoffeeMachineView.class, new RouteParameters("name", name));
        layout.add(link);

        final Label status = new Label();
        layout.add(status);

        this.service.getStatusOfDigitalTwin(name).onComplete(r -> {
            if (r.succeeded())
                this.updateUI(() -> status.setText(STATUS_TEXT + r.result()));
            else
                this.updateUI(() -> status.setText(STATUS_TEXT + Status.OUT_OF_SERVICE));
            this.latch.countDown();
        });

        this.service.subscribeToStatusChanged(name,
                s -> this.updateUI(() -> status.setText(STATUS_TEXT + s)),
                unused -> this.updateUI(() -> status.setText(STATUS_TEXT + Status.OUT_OF_SERVICE)));

        add(layout);
    }
}
