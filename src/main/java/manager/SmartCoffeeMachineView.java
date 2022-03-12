package manager;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;
import common.Glasses;
import common.Resource;
import common.ResourceImpl;
import common.Sugar;

import java.util.List;

@Route("machine/:name")
public class SmartCoffeeMachineView extends HorizontalLayout implements BeforeEnterObserver {

    private final DashBoardService service;
    private final UI ui;
    private final Label status;
    private final Grid<Resource> resources;

    public SmartCoffeeMachineView() {
        this.service = VaadinSession.getCurrent().getAttribute(DashBoardService.class);
        this.ui = UI.getCurrent();
        this.status = new Label();

        this.resources = new Grid<>(Resource.class, true);

        add(this.status, this.resources);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        final String name = event.getRouteParameters().get("name").orElse(null);

        /*digitalTwin.getStatus().onComplete(res -> {
            if (res.succeeded())
                ui.access(() -> this.status.setText(res.result()));
            else
                ui.access(() -> this.status.setText("Unreachable"));
        });*/
        GridListDataView<Resource> resources = this.resources.setItems(this.service.getResourcesOfDigitalTwin(name));

        //digitalTwin.subscribeToStatusChanged(s -> ui.access(() -> this.status.setText(s)), s -> ui.access(() -> this.status.setText("Out of service")));
        resources.setIdentifierProvider(Resource::getName);
        this.service.subscribeToRemaining(name, json -> {
            String product = json.getString("product");
            int remaining = json.getInteger("remaining");
            int sugarRemaining = json.getInteger("sugarRemaining");
            int glassesRemaining = json.getInteger("glassesRemaining");

            updateUI(() -> {
                resources.refreshItem(new ResourceImpl(product, remaining));
                resources.refreshItem(new Sugar(sugarRemaining));
                resources.refreshItem(new Glasses(glassesRemaining));
            });
        });
    }

    private void updateUI(Command command) {
        this.ui.access(command);
    }
}
