package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

public class OrderGui extends JDialog {

    private JButton addSugar;
    private JButton subSugar;
    private JLabel sugarLevel;
    private JPanel panel;
    private JButton order;
    private JLabel availability;
    private int sugarLevelValue = 2;

    public OrderGui(JFrame frame, String product, Consumer<String> availabilityConsumer) {
        super(frame, true);
        this.addSugar.addActionListener(e -> {
            if (this.sugarLevelValue < 5) {
                this.sugarLevelValue++;
                this.sugarLevel.setText(String.valueOf(this.sugarLevelValue));
            }
        });
        this.subSugar.addActionListener(e -> {
            if (this.sugarLevelValue > 0) {
                this.sugarLevelValue--;
                this.sugarLevel.setText(String.valueOf(this.sugarLevelValue));
            }
        });
        this.availability.setBorder(new EmptyBorder(5, 0, 10, 0));
        availabilityConsumer.accept(product);
        setSize(200, 200);
        setTitle(product.toUpperCase());
        setResizable(false);
        setLocationRelativeTo(null);
        getContentPane().add(this.panel);
    }

    public void setMakeAction(Consumer<Integer> consumer) {
        this.order.addActionListener(e -> {
            consumer.accept(this.sugarLevelValue);
            this.dispose();
        });
    }

    public void setAvailability(String availability) {
        this.availability.setText("Remaining products: " + availability);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        addSugar = new JButton();
        addSugar.setText("Add Sugar");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0.5;
        panel.add(addSugar, gbc);
        sugarLevel = new JLabel();
        sugarLevel.setHorizontalAlignment(0);
        sugarLevel.setHorizontalTextPosition(0);
        sugarLevel.setText("2");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(sugarLevel, gbc);
        subSugar = new JButton();
        subSugar.setText("Sub Sugar");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.5;
        gbc.weighty = 0.5;
        panel.add(subSugar, gbc);
        order = new JButton();
        order.setText("Order");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panel.add(order, gbc);
        availability = new JLabel();
        availability.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(availability, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel;
    }

}
