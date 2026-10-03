package com.example.game;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/** Главное меню: «СОЗДАТЬ ХОСТ» и «ПОДКЛЮЧИТЬСЯ». */
public class MenuPanel extends JPanel {

    private final JTextField hostField = new JTextField("localhost");
    private final JTextField portField = new JTextField("5000");

    public MenuPanel(IntConsumer onHost, BiConsumer<String, Integer> onJoin, java.util.function.Consumer<String> onError) {
        setPreferredSize(new Dimension(World.WIDTH, World.HEIGHT));
        setBackground(new Color(30, 30, 40));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(120, 250, 0, 250));

        JLabel title = new JLabel("SWING GAME");
        title.setFont(new Font("SansSerif", Font.BOLD, 40));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JPanel fields = new JPanel(new GridLayout(2, 2, 8, 8));
        fields.setOpaque(false);
        fields.add(label("Адрес:"));
        fields.add(hostField);
        fields.add(label("Порт:"));
        fields.add(portField);
        fields.setMaximumSize(new Dimension(300, 70));

        JButton hostButton = new JButton("СОЗДАТЬ ХОСТ");
        JButton joinButton = new JButton("ПОДКЛЮЧИТЬСЯ");
        for (JButton b : new JButton[]{hostButton, joinButton}) {
            b.setAlignmentX(CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(300, 50));
            b.setFont(new Font("SansSerif", Font.BOLD, 18));
            b.setFocusable(false);
        }

        hostButton.addActionListener(e -> {
            Integer port = parsePort(onError);
            if (port != null) onHost.accept(port);
        });
        joinButton.addActionListener(e -> {
            Integer port = parsePort(onError);
            if (port != null) onJoin.accept(hostField.getText().trim(), port);
        });

        add(title);
        add(Box.createVerticalStrut(40));
        add(hostButton);
        add(Box.createVerticalStrut(30));
        add(fields);
        add(Box.createVerticalStrut(15));
        add(joinButton);
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.LIGHT_GRAY);
        return l;
    }

    private Integer parsePort(java.util.function.Consumer<String> onError) {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) throw new NumberFormatException();
            return port;
        } catch (NumberFormatException ex) {
            onError.accept("Порт должен быть числом от 1 до 65535");
            return null;
        }
    }
}
