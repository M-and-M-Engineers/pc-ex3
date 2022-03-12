package client;

public class App {

    public static void main(String[] args) {
        MainGui mainGui = new MainGui();
        new Controller(mainGui, Integer.parseInt(args[0]));
        mainGui.setVisible(true);
    }
}
