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

@Route("machine/:name")
public class SmartCoffeeMachineView extends NotifiableView implements BeforeEnterObserver, HasDynamicTitle {

    private static final String STATUS_TEXT = "Status -> ";
    private final DashBoardService service;
    private final H1 heading;
    private final Label status;
    private final Grid<Resource> resources;
    private String name;

    public SmartCoffeeMachineView() {
        super();
        this.service = VaadinSession.getCurrent().getAttribute(DashBoardService.class);

        this.heading = new H1();

        this.status = new Label();

        this.resources = new Grid<>(Resource.class, true);
        this.resources.setAllRowsVisible(true);
        this.resources.setWidth("35%");
        this.resources.setSelectionMode(Grid.SelectionMode.NONE);
        this.resources.getColumns().forEach(c -> {
            c.setSortable(false);
            c.setTextAlign(ColumnTextAlign.CENTER);
        });
        this.resources.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        add(this.heading, this.status, this.resources);

        setHorizontalComponentAlignment(Alignment.CENTER, this.resources);
        setAlignItems(Alignment.CENTER);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        this.name = event.getRouteParameters().get("name").orElse(null);

        this.heading.setText(this.name);

        this.service.getStatusOfDigitalTwin(this.name).onComplete(r -> {
            if (r.succeeded()) {
                this.updateUI(() -> this.resources.setItems(this.service.getResourcesOfDigitalTwin(this.name))
                        .setIdentifierProvider(Resource::getName));

                this.updateUI(() -> this.status.setText(STATUS_TEXT + r.result()));
            } else {
                this.updateUI(() -> this.status.setText(STATUS_TEXT + Status.OUT_OF_SERVICE));
            }
        });

        this.service.subscribeToStatusChanged(this.name,
                s -> this.updateUI(() -> this.status.setText(STATUS_TEXT + s)),
                unused -> this.updateUI(() -> this.status.setText(STATUS_TEXT + Status.OUT_OF_SERVICE)));

        this.service.subscribeToServed(this.name, json -> {
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
        return "Smart Coffee Machine " + this.name;
    }
}
