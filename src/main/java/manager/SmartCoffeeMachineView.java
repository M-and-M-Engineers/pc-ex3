package manager;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import scm.*;

@Route("machine/:port")
public class SmartCoffeeMachineView extends NotifiableView implements BeforeEnterObserver, HasDynamicTitle {

    private static final String STATUS_TEXT = "State -> ";
    private final DashBoardService service;
    private final H1 heading;
    private final Label state;
    private final Grid<Resource> resources;
    private int port;

    public SmartCoffeeMachineView() {
        this.service = VaadinSession.getCurrent().getAttribute(DashBoardService.class);

        this.heading = new H1();

        this.state = new Label();

        this.resources = new Grid<>(Resource.class, true);
        this.resources.setAllRowsVisible(true);
        this.resources.setWidth("35%");
        this.resources.setSelectionMode(Grid.SelectionMode.NONE);
        this.resources.getColumns().forEach(c -> {
            c.setSortable(false);
            c.setTextAlign(ColumnTextAlign.CENTER);
        });
        this.resources.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        add(this.heading, this.state, this.resources);

        setHorizontalComponentAlignment(Alignment.CENTER, this.resources);
        setAlignItems(Alignment.CENTER);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        this.port = event.getRouteParameters().getInteger("port").orElse(0);

        this.heading.setText(this.service.getNames().get(this.port));

        final String state = this.service.getStates(this.port);
        this.state.setText(STATUS_TEXT + state);

        this.resources.setItems(this.service.getResources(this.port))
                .setIdentifierProvider(Resource::getName);

        this.service.subscribeToStateChanged(this.port,
                s -> this.updateUI(() -> this.state.setText(STATUS_TEXT + s)),
                unused -> this.updateUI(() -> this.state.setText(STATUS_TEXT + State.OUT_OF_SERVICE)));

        this.service.subscribeToServed(this.port, json -> {
            final String product = json.getString("product");
            final int remaining = json.getInteger("remaining");
            final int sugarRemaining = json.getInteger("sugarRemaining");
            final int glassesRemaining = json.getInteger("glassesRemaining");

            this.updateUI(() -> {
                this.resources.getListDataView().refreshItem(new ResourceImpl(product, remaining));
                this.resources.getListDataView().refreshItem(new Sugar(sugarRemaining));
                this.resources.getListDataView().refreshItem(new Glasses(glassesRemaining));
            });
        });
    }

    @Override
    public String getPageTitle() {
        return "Smart Coffee Machine " + this.service.getNames().get(this.port);
    }
}
