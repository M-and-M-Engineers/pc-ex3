package manager;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import scm.State;

import java.util.Map;

@Route("/")
@PageTitle("DashBoard")
public class DashBoardView extends NotifiableView {

    private static final String STATUS_TEXT = "State -> ";
    private final DashBoardService service;

    public DashBoardView() {
        this.service = VaadinSession.getCurrent().getAttribute(DashBoardService.class);

        final H1 heading = new H1("Manager DashBoard");
        add(heading);

        final Map<Integer, String> digitalTwinNames = this.service.getNames();
        digitalTwinNames.forEach(this::addNewElement);
        setAlignItems(Alignment.CENTER);
    }

    private void addNewElement(final int port, final String name) {
        final HorizontalLayout layout = new HorizontalLayout();

        final RouterLink link = new RouterLink(name, SmartCoffeeMachineView.class, new RouteParameters("port", String.valueOf(port)));
        layout.add(link);

        final Label state = new Label();
        layout.add(state);

        state.setText(STATUS_TEXT + this.service.getStates(port));

        this.service.subscribeToStateChanged(port,
                s -> this.updateUI(() -> state.setText(STATUS_TEXT + s)),
                unused -> this.updateUI(() -> state.setText(STATUS_TEXT + State.OUT_OF_SERVICE)));

        add(layout);
    }
}
