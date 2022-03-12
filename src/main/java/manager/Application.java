package manager;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinSession;

@PWA(name = "Project Base for Vaadin", shortName = "Project Base")
@Push
public class Application implements AppShellConfigurator {

    public Application() {
        final DashBoardService service = new DashBoardService();
        VaadinSession.getCurrent().setAttribute(DashBoardService.class, service);
    }
}
