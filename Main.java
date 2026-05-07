import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Main extends JFrame {
    private JPanel buttonPanel;
    private CanvasPanel canvasPanel;
    private JButton lastActiveButton;
    private JButton previousActiveButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }

    public Main() {
        setTitle("OOPS UML Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initializeComponents();
        setupMenuBar();

        add(buttonPanel, BorderLayout.WEST);
        add(canvasPanel, BorderLayout.CENTER);
    }

    private void initializeComponents() {
        canvasPanel = new CanvasPanel();
        canvasPanel.setBackground(Color.LIGHT_GRAY);

        buttonPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        buttonPanel.setPreferredSize(new Dimension(120, 0));

        String[] labels = {"select", "association", "generalization", "composition", "rect", "oval"};
        for (String label : labels) {
            JButton button = new JButton(label);
            button.addMouseListener(new ToolButtonHandler(button));
            buttonPanel.add(button);
            if ("select".equals(label)) {
                lastActiveButton = button;
                setButtonActive(button, true);
            }
        }
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        Font menuFont = new Font("Arial", Font.PLAIN, 14);

        JMenu fileMenu = new JMenu("File");
        fileMenu.setFont(menuFont);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setFont(menuFont);

        JMenuItem groupItem = new JMenuItem("Group");
        JMenuItem ungroupItem = new JMenuItem("Ungroup");
        JMenuItem labelItem = new JMenuItem("Label");

        groupItem.addActionListener(e -> canvasPanel.groupSelected());
        ungroupItem.addActionListener(e -> canvasPanel.ungroupSelected());
        labelItem.addActionListener(e -> showLabelDialog());

        editMenu.add(groupItem);
        editMenu.add(ungroupItem);
        editMenu.add(labelItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        setJMenuBar(menuBar);
    }

    private void showLabelDialog() {
        if (canvasPanel.getSelectedShapes().size() != 1) {
            JOptionPane.showMessageDialog(this, "Please select exactly one basic object.");
            return;
        }

        UMLObject object = canvasPanel.getSelectedShapes().get(0);
        if (object instanceof CompositeShape) {
            JOptionPane.showMessageDialog(this, "Label can only be customized for a basic object.");
            return;
        }

        JTextField nameField = new JTextField(object.getName());
        JButton colorButton = new JButton("Choose");
        final Color[] selectedColor = {object.getLabelColor()};
        colorButton.setBackground(selectedColor[0]);
        colorButton.addActionListener(e -> {
            Color color = JColorChooser.showDialog(this, "Choose Label Color", selectedColor[0]);
            if (color != null) {
                selectedColor[0] = color;
                colorButton.setBackground(color);
            }
        });

        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Label Name"));
        panel.add(nameField);
        panel.add(new JLabel("Label Color"));
        panel.add(colorButton);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Customize Label Style",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            object.setName(nameField.getText());
            object.setLabelColor(selectedColor[0]);
            canvasPanel.repaint();
        }
    }

    private void setButtonActive(JButton button, boolean active) {
        if (active) {
            button.setBackground(Color.BLACK);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(UIManager.getColor("Button.background"));
            button.setForeground(UIManager.getColor("Button.foreground"));
        }
    }

    private void clearButtonStates() {
        for (Component component : buttonPanel.getComponents()) {
            if (component instanceof JButton) {
                setButtonActive((JButton) component, false);
            }
        }
    }

    private void activateTool(JButton button) {
        clearButtonStates();
        setButtonActive(button, true);
        lastActiveButton = button;
        canvasPanel.setCurrentMode(button.getText());
    }

    private class ToolButtonHandler extends MouseAdapter {
        private final JButton button;

        ToolButtonHandler(JButton button) {
            this.button = button;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            String mode = button.getText();
            if ("rect".equals(mode) || "oval".equals(mode)) {
                previousActiveButton = lastActiveButton;
                clearButtonStates();
                setButtonActive(button, true);
                canvasPanel.setCurrentMode(mode);
            } else {
                previousActiveButton = null;
                activateTool(button);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            String mode = button.getText();
            if (!"rect".equals(mode) && !"oval".equals(mode)) {
                return;
            }

            Point canvasPoint = SwingUtilities.convertPoint(button, e.getPoint(), canvasPanel);
            if (canvasPanel.contains(canvasPoint)) {
                canvasPanel.createShapeAt(mode, canvasPoint);
            }

            clearButtonStates();
            if (previousActiveButton != null) {
                setButtonActive(previousActiveButton, true);
                lastActiveButton = previousActiveButton;
                canvasPanel.setCurrentMode(previousActiveButton.getText());
            } else {
                Component first = buttonPanel.getComponent(0);
                if (first instanceof JButton) {
                    lastActiveButton = (JButton) first;
                    setButtonActive(lastActiveButton, true);
                    canvasPanel.setCurrentMode("select");
                }
            }
            previousActiveButton = null;
        }
    }
}
