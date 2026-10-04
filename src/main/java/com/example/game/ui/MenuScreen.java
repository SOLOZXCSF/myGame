package com.example.game.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Главное меню.
 * Swing-кнопки рисуются поверх canvas через OverlayLayout.
 * Меню не знает о JFrame — оно только реализует Screen.
 */
public class MenuScreen implements Screen {

    private final IntConsumer              onHost;
    private final BiConsumer<String,Integer> onJoin;
    private final Consumer<String>         onError;

    private final JTextField hostField = new JTextField("localhost");
    private final JTextField portField = new JTextField("5000");

    // Swing-overlay: добавляется/убирается через ScreenManager.Canvas
    private JPanel overlay;

    public MenuScreen(IntConsumer onHost,
                      BiConsumer<String,Integer> onJoin,
                      Consumer<String> onError) {
        this.onHost  = onHost;
        this.onJoin  = onJoin;
        this.onError = onError;
    }

    @Override
    public void onEnable() {
        overlay = buildOverlay();
    }

    @Override
    public void onDisable() {
        overlay = null;
    }

    /** Вернуть панель с кнопками для добавления поверх canvas (Main вызывает это). */
    public JPanel getOverlay() { return overlay; }

    @Override
    public void update(double dt) { /* меню статично */ }

    @Override
    public void render(Graphics2D g, int w, int h) {
        // тёмный фон
        g.setColor(new Color(30, 30, 40));
        g.fillRect(0, 0, w, h);

        // градиентный заголовок
        GradientPaint gp = new GradientPaint(0, h / 2 - 180, new Color(80,120,255),
                                              w, h / 2 - 80,  new Color(180,80,255));
        g.setPaint(gp);
        g.setFont(new Font("SansSerif", Font.BOLD, 52));
        String title = "SWING GAME";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (w - tw) / 2, h / 2 - 110);

        // подсказка внизу
        g.setPaint(new Color(120, 120, 140));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        String hint = "WASD — движение   ЛКМ / ПРОБЕЛ — огонь   мышь — прицел   ESC — меню";
        int hw = g.getFontMetrics().stringWidth(hint);
        g.drawString(hint, (w - hw) / 2, h - 20);
    }

    private JPanel buildOverlay() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(220, 250, 0, 250));

        JButton hostBtn = styledButton("СОЗДАТЬ ХОСТ");
        JButton joinBtn = styledButton("ПОДКЛЮЧИТЬСЯ");

        JPanel fields = new JPanel(new GridLayout(2, 2, 8, 8));
        fields.setOpaque(false);
        fields.add(label("Адрес:")); fields.add(hostField);
        fields.add(label("Порт:"));  fields.add(portField);
        fields.setMaximumSize(new Dimension(300, 70));
        fields.setAlignmentX(Component.CENTER_ALIGNMENT);

        hostBtn.addActionListener(e -> {
            Integer p = parsePort(); if (p != null) onHost.accept(p);
        });
        joinBtn.addActionListener(e -> {
            Integer p = parsePort();
            if (p != null) onJoin.accept(hostField.getText().trim(), p);
        });

        panel.add(hostBtn);
        panel.add(Box.createVerticalStrut(16));
        panel.add(fields);
        panel.add(Box.createVerticalStrut(10));
        panel.add(joinBtn);
        return panel;
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(300, 48));
        b.setFont(new Font("SansSerif", Font.BOLD, 18));
        b.setFocusable(false);
        return b;
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(Color.LIGHT_GRAY);
        return l;
    }

    private Integer parsePort() {
        try {
            int p = Integer.parseInt(portField.getText().trim());
            if (p < 1 || p > 65535) throw new NumberFormatException();
            return p;
        } catch (NumberFormatException ex) {
            onError.accept("Порт должен быть числом от 1 до 65535");
            return null;
        }
    }
}
