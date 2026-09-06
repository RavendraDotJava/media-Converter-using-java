import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegGUI extends JFrame {

    private JTextField inputField;
    private JTextField outputField;
    private JTextArea logArea;
    private JProgressBar progressBar;

    public FFmpegGUI() {
        setTitle("FFmpeg Converter");
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridLayout(3, 3, 5, 5));

        inputField = new JTextField();
        outputField = new JTextField();

        JButton inputBtn = new JButton("Browse");
        JButton outputBtn = new JButton("Save As");
        JButton convertBtn = new JButton("Convert");

        inputBtn.addActionListener(e -> chooseFile(inputField));
        outputBtn.addActionListener(e -> saveFile(outputField));
        convertBtn.addActionListener(this::runFFmpeg);

        panel.add(new JLabel("Input File:"));
        panel.add(inputField);
        panel.add(inputBtn);

        panel.add(new JLabel("Output File:"));
        panel.add(outputField);
        panel.add(outputBtn);

        panel.add(new JLabel(""));
        panel.add(convertBtn);

        add(panel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // ✅ Progress Bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.SOUTH);
    }

    private void chooseFile(JTextField field) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            field.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void saveFile(JTextField field) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            field.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void runFFmpeg(ActionEvent e) {
        String input = inputField.getText();
        String output = outputField.getText();

        if (input.isEmpty() || output.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select files first!");
            return;
        }

        progressBar.setValue(0);
        logArea.setText("Starting...\n");

        new Thread(() -> {
            try {
                String ffmpegPath = getFFmpegPath();

                // 🔥 Step 1: Get duration
                double totalDuration = getVideoDuration(ffmpegPath, input);

                ProcessBuilder pb = new ProcessBuilder(
                        ffmpegPath,
                        "-i", input,
                        "-vn",
                        "-acodec", "mp3",
                        output
                );

                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                );

                String line;
                Pattern timePattern = Pattern.compile("time=([0-9:.]+)");

                while ((line = reader.readLine()) != null) {
                    logArea.append(line + "\n");

                    Matcher matcher = timePattern.matcher(line);
                    if (matcher.find()) {
                        double currentTime = parseTime(matcher.group(1));

                        int progress = (int) ((currentTime / totalDuration) * 100);
                        progressBar.setValue(Math.min(progress, 100));
                    }
                }

                int exitCode = process.waitFor();
                progressBar.setValue(100);
                logArea.append("\n✅ Finished: " + exitCode);

            } catch (Exception ex) {
                logArea.append("\n❌ Error: " + ex.getMessage());
            }
        }).start();
    }

    // 🔥 Get total video duration
    private double getVideoDuration(String ffmpegPath, String input) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-i", input);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        String line;
        Pattern pattern = Pattern.compile("Duration: ([0-9:.]+)");

        while ((line = reader.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return parseTime(matcher.group(1));
            }
        }

        throw new RuntimeException("Could not get duration!");
    }

    // 🔥 Convert HH:MM:SS.ms → seconds
    private double parseTime(String time) {
        String[] parts = time.split(":");
        double seconds = 0;

        seconds += Double.parseDouble(parts[0]) * 3600;
        seconds += Double.parseDouble(parts[1]) * 60;
        seconds += Double.parseDouble(parts[2]);

        return seconds;
    }

    private String getFFmpegPath() {
        String base = System.getProperty("user.dir");

        String pathUnix = base + "/ffmpeg/ffmpeg";
        String pathWin = base + "\\ffmpeg\\ffmpeg.exe";

        if (new File(pathUnix).exists()) return pathUnix;
        if (new File(pathWin).exists()) return pathWin;

        throw new RuntimeException("FFmpeg not found!");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FFmpegGUI().setVisible(true));
    }
}