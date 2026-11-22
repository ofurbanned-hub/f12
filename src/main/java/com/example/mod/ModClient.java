package com.example.mod;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

/** Owner-controlled demonstration. Minecraft startup NEVER starts a Windows shutdown. */
public final class ModClient implements PreLaunchEntrypoint {
    private static final Path CONFIG = Path.of(System.getProperty("user.home"), ".minecraft-mod-prank.properties");
    private static final Properties SETTINGS = new Properties();
    private static JFrame frame;
    private static JLabel status;
    private static JButton start;
    private static JButton cancel;
    private static Timer timer;
    private static boolean pending;
    private static int secondsLeft;

    @Override public void onPreLaunch() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        loadSettings();
        if (!Boolean.parseBoolean(SETTINGS.getProperty("enabled", "true"))) return;
        SwingUtilities.invokeLater(ModClient::openControl);
    }

    private static void loadSettings() {
        SETTINGS.setProperty("enabled", "true");
        SETTINGS.setProperty("message", "Say bye to ur pc :) - Press F12 to cancel");
        SETTINGS.setProperty("typing_speed_ms", "120");
        SETTINGS.setProperty("countdown_seconds", "30");
        SETTINGS.setProperty("shutdown_enabled", "false");
        try {
            if (Files.exists(CONFIG)) {
                try (var in = Files.newInputStream(CONFIG)) { SETTINGS.load(in); }
            } else {
                try (var out = Files.newOutputStream(CONFIG, StandardOpenOption.CREATE_NEW)) {
                    SETTINGS.store(out, "Notepad prank settings. Shutdown requires owner to explicitly start it in the control window.");
                }
            }
        } catch (IOException ex) { System.err.println("Could not load mod settings: " + ex); }
    }

    private static int configuredSeconds() {
        try { return Math.max(30, Integer.parseInt(SETTINGS.getProperty("countdown_seconds", "30"))); }
        catch (NumberFormatException ex) { return 30; }
    }

    private static void openControl() {
        String message = SETTINGS.getProperty("message", "Say bye to ur pc :) - Press F12 to cancel");
        openNotepad(message);
        frame = new JFrame("Mod — Owner controls");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(480, 235);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JTextArea explanation = new JTextArea("Minecraft startup does NOT shut down Windows.\n" +
                "Shutdown is optional and must be started here by the PC owner.\n" +
                "Press F12 while this window is focused, or click Cancel, to abort.");
        explanation.setEditable(false);
        explanation.setOpaque(false);
        explanation.setLineWrap(true);
        explanation.setWrapStyleWord(true);
        panel.add(explanation, BorderLayout.NORTH);
        status = new JLabel("No shutdown scheduled.");
        panel.add(status, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        start = new JButton("Start Windows shutdown (30+ sec)");
        cancel = new JButton("Cancel shutdown (F12)");
        cancel.setEnabled(false);
        start.setEnabled(Boolean.parseBoolean(SETTINGS.getProperty("shutdown_enabled", "false")));
        if (!start.isEnabled()) status.setText("Shutdown disabled in configuration (shutdown_enabled=false).");
        start.addActionListener(e -> beginShutdown());
        cancel.addActionListener(e -> abortShutdown());
        buttons.add(cancel); buttons.add(start);
        panel.add(buttons, BorderLayout.SOUTH);
        frame.setContentPane(panel);
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "cancel");
        frame.getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { abortShutdown(); }
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (pending) abortShutdown();
                frame.dispose();
            }
        });
        frame.setVisible(true);
        frame.toFront();
    }

    private static void openNotepad(String message) {
        try {
            Path file = Files.createTempFile("mod-prank-", ".txt");
            Files.writeString(file, message + System.lineSeparator(), StandardCharsets.UTF_8);
            file.toFile().deleteOnExit();
            new ProcessBuilder("notepad.exe", file.toString()).start();
        } catch (IOException ex) { System.err.println("Notepad could not open: " + ex); }
    }

    private static void beginShutdown() {
        if (pending || !Boolean.parseBoolean(SETTINGS.getProperty("shutdown_enabled", "false"))) return;
        secondsLeft = configuredSeconds();
        try {
            Process process = new ProcessBuilder("shutdown.exe", "/s", "/t", String.valueOf(secondsLeft)).start();
            if (process.waitFor() != 0) {
                status.setText("Windows rejected the shutdown request.");
                return;
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            status.setText("Could not schedule shutdown: " + ex.getMessage());
            return;
        }
        pending = true;
        start.setEnabled(false);
        cancel.setEnabled(true);
        status.setText("Windows shutdown pending: " + secondsLeft + " seconds. F12 or Cancel.");
        timer = new Timer(1000, e -> {
            secondsLeft--;
            if (secondsLeft <= 0) {
                timer.stop();
                pending = false;
                cancel.setEnabled(false);
                status.setText("Shutdown countdown completed.");
            } else status.setText("Windows shutdown pending: " + secondsLeft + " seconds. F12 or Cancel.");
        });
        timer.start();
    }

    private static void abortShutdown() {
        if (!pending) return;
        try {
            Process process = new ProcessBuilder("shutdown.exe", "/a").start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                status.setText("Abort was not confirmed by Windows. Try 'shutdown /a' immediately.");
                return;
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            status.setText("Abort failed. Try 'shutdown /a' immediately.");
            return;
        }
        pending = false;
        if (timer != null) timer.stop();
        cancel.setEnabled(false);
        start.setEnabled(true);
        status.setText("Windows shutdown canceled.");
    }
}
