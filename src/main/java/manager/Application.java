package manager;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@PWA(name = "Manager DashBoard", shortName = "Manager DashBoard")
@Push
@Theme(themeClass = Lumo.class, variant = Lumo.DARK)
@PreserveOnRefresh
public class Application implements AppShellConfigurator {

    public Application() {
        final DashBoardService service = new DashBoardService();
        VaadinSession.getCurrent().setAttribute(DashBoardService.class, service);
    }
}
