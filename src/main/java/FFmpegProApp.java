import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class FFmpegProApp extends Application {

    // ─── UI Components ────────────────────────────────────────────
    private TextField inputField;
    private TextField outputField;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Slider crfSlider;
    private Label crfValueLabel;
    private TextArea logArea;
    private ListView<QueueItem> queueList;
    private ComboBox<String> codecBox;
    private ComboBox<String> presetBox;
    private ComboBox<String> resolutionBox;
    private Button startBtn;
    private Button cancelBtn;
    private Label statusLabel;

    // ─── State ────────────────────────────────────────────────────
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final ObservableList<QueueItem> queueItems = FXCollections.observableArrayList();
    private volatile Process currentProcess;
    private volatile boolean isCancelled = false;

    // ─── Queue Item Model ─────────────────────────────────────────
    static class QueueItem {
        final String input;
        final String output;
        String status; // PENDING, RUNNING, DONE, ERROR, CANCELLED

        QueueItem(String input, String output) {
            this.input = input;
            this.output = output;
            this.status = "PENDING";
        }

        @Override
        public String toString() {
            String icon = switch (status) {
                case "RUNNING"   -> "⏳";
                case "DONE"      -> "✅";
                case "ERROR"     -> "❌";
                case "CANCELLED" -> "🚫";
                default          -> "🕐";
            };
            String inputName = Paths.get(input).getFileName().toString();
            String outputName = Paths.get(output).getFileName().toString();
            return icon + "  " + inputName + "  →  " + outputName;
        }
    }

    // ─── App Entry ────────────────────────────────────────────────
    @Override
    public void start(Stage stage) {
        stage.setTitle("FFmpeg Pro Tool");
        stage.setMinWidth(760);
        stage.setMinHeight(680);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0f0f13;");

        root.getChildren().addAll(
                buildHeader(),
                buildBody()
        );

        Scene scene = new Scene(root, 820, 740);
        scene.setFill(Color.web("#0f0f13"));
        stage.setScene(scene);
        stage.show();

        log("Ready. Select a file or drag & drop to begin.", "INFO");
    }

    // ─── Header ───────────────────────────────────────────────────
    private HBox buildHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.setStyle("-fx-background-color: #16161e; -fx-border-color: #2a2a3a; -fx-border-width: 0 0 1 0;");

        Label icon = new Label("⚡");
        icon.setStyle("-fx-font-size: 22px;");

        Label title = new Label("FFmpeg Pro");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e0e0f0; -fx-font-family: 'Courier New';");

        Label version = new Label("v2.0");
        version.setStyle("-fx-font-size: 11px; -fx-text-fill: #5a5a7a; -fx-padding: 4 8 4 8; " +
                "-fx-background-color: #1e1e2e; -fx-background-radius: 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("● Idle");
        statusLabel.setStyle("-fx-text-fill: #5a5a7a; -fx-font-size: 12px;");

        header.getChildren().addAll(icon, gap(8), title, gap(10), version, spacer, statusLabel);
        return header;
    }

    // ─── Body ─────────────────────────────────────────────────────
    private HBox buildBody() {
        HBox body = new HBox(0);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox left = buildLeftPanel();
        VBox right = buildRightPanel();

        HBox.setHgrow(left, Priority.ALWAYS);
        body.getChildren().addAll(left, right);
        return body;
    }

    // ─── Left Panel ───────────────────────────────────────────────
    private VBox buildLeftPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(20, 20, 20, 20));
        panel.setStyle("-fx-background-color: #0f0f13;");
        VBox.setVgrow(panel, Priority.ALWAYS);

        panel.getChildren().addAll(
                buildFileSection(),
                buildSettingsSection(),
                buildProgressSection(),
                buildLogSection()
        );

        return panel;
    }

    // ─── File Section ─────────────────────────────────────────────
    private VBox buildFileSection() {
        VBox box = new VBox(10);

        inputField = new TextField();
        inputField.setPromptText("Drop a video file here or browse...");
        styleTextField(inputField);
        setupDragDrop(inputField);

        outputField = new TextField();
        outputField.setPromptText("Output path...");
        styleTextField(outputField);

        Button browseInput = styledButton("📂 Browse", "#1e1e2e", "#4a9eff");
        Button browseOutput = styledButton("💾 Save As", "#1e1e2e", "#4a9eff");
        Button addQueue = styledButton("+ Add to Queue", "#1a2a1a", "#4aff88");

        browseInput.setOnAction(e -> chooseInputFile());
        browseOutput.setOnAction(e -> chooseSaveFile());
        addQueue.setOnAction(e -> addToQueue());

        HBox inputRow = new HBox(8, inputField, browseInput);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        HBox outputRow = new HBox(8, outputField, browseOutput);
        HBox.setHgrow(outputField, Priority.ALWAYS);

        HBox addRow = new HBox();
        addRow.setAlignment(Pos.CENTER_RIGHT);
        addRow.getChildren().add(addQueue);

        box.getChildren().addAll(
                sectionLabel("INPUT / OUTPUT"),
                inputRow,
                outputRow,
                addRow
        );
        return box;
    }

    // ─── Settings Section ─────────────────────────────────────────
    private VBox buildSettingsSection() {
        VBox box = new VBox(10);

        // Codec
        codecBox = new ComboBox<>(FXCollections.observableArrayList(
                "libx264 (H.264)", "libx265 (H.265/HEVC)", "libvpx-vp9 (VP9)", "copy (No Re-encode)"
        ));
        codecBox.setValue("libx264 (H.264)");
        styleComboBox(codecBox);

        // Preset
        presetBox = new ComboBox<>(FXCollections.observableArrayList(
                "ultrafast", "superfast", "veryfast", "faster", "fast",
                "medium", "slow", "slower", "veryslow"
        ));
        presetBox.setValue("medium");
        styleComboBox(presetBox);

        // Resolution
        resolutionBox = new ComboBox<>(FXCollections.observableArrayList(
                "Original", "3840x2160 (4K)", "1920x1080 (1080p)",
                "1280x720 (720p)", "854x480 (480p)", "640x360 (360p)"
        ));
        resolutionBox.setValue("Original");
        styleComboBox(resolutionBox);

        // CRF
        crfSlider = new Slider(0, 51, 23);
        crfSlider.setShowTickMarks(true);
        crfSlider.setMajorTickUnit(10);
        crfSlider.setStyle("-fx-control-inner-background: #1e1e2e;");

        crfValueLabel = new Label("CRF: 23");
        crfValueLabel.setStyle("-fx-text-fill: #4a9eff; -fx-font-size: 12px; -fx-font-family: 'Courier New';");
        crfSlider.valueProperty().addListener((obs, o, n) ->
                crfValueLabel.setText("CRF: " + (int) crfSlider.getValue() + getQualityHint((int) crfSlider.getValue()))
        );

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(miniLabel("Codec"), 0, 0);
        grid.add(codecBox, 1, 0);
        grid.add(miniLabel("Preset"), 0, 1);
        grid.add(presetBox, 1, 1);
        grid.add(miniLabel("Resolution"), 0, 2);
        grid.add(resolutionBox, 1, 2);
        ColumnConstraints c0 = new ColumnConstraints(80);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setFillWidth(true);
        grid.getColumnConstraints().addAll(c0, c1);
        codecBox.setMaxWidth(Double.MAX_VALUE);
        presetBox.setMaxWidth(Double.MAX_VALUE);
        resolutionBox.setMaxWidth(Double.MAX_VALUE);

        HBox crfRow = new HBox(10, crfSlider, crfValueLabel);
        HBox.setHgrow(crfSlider, Priority.ALWAYS);
        crfRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(
                sectionLabel("ENCODING SETTINGS"),
                grid,
                miniLabel("Quality (CRF)"),
                crfRow
        );
        return box;
    }

    // ─── Progress Section ─────────────────────────────────────────
    private VBox buildProgressSection() {
        VBox box = new VBox(8);

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);
        progressBar.setStyle(
                "-fx-accent: #4a9eff;" +
                        "-fx-control-inner-background: #1e1e2e;" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-radius: 5;"
        );

        progressLabel = new Label("0%");
        progressLabel.setStyle("-fx-text-fill: #5a5a7a; -fx-font-size: 11px; -fx-font-family: 'Courier New';");
        progressBar.progressProperty().addListener((obs, o, n) -> {
            double pct = n.doubleValue();
            if (pct >= 0) progressLabel.setText(String.format("%.1f%%", pct * 100));
        });

        startBtn = styledButton("▶ Start Conversion", "#0d2a1a", "#4aff88");
        cancelBtn = styledButton("■ Cancel", "#2a0d0d", "#ff4a4a");
        cancelBtn.setDisable(true);

        startBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setMaxWidth(Double.MAX_VALUE);

        startBtn.setOnAction(e -> startConversion());
        cancelBtn.setOnAction(e -> cancelConversion());

        HBox btnRow = new HBox(10, startBtn, cancelBtn);
        btnRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(startBtn, Priority.ALWAYS);
        HBox.setHgrow(cancelBtn, Priority.ALWAYS);

        HBox progRow = new HBox(10, progressBar, progressLabel);
        progRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        box.getChildren().addAll(progRow, btnRow);
        return box;
    }

    // ─── Log Section ──────────────────────────────────────────────
    private VBox buildLogSection() {
        VBox box = new VBox(6);
        VBox.setVgrow(box, Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Button clearBtn = new Button("Clear");
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #5a5a7a; " +
                "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
        clearBtn.setOnAction(e -> logArea.clear());
        Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
        header.getChildren().addAll(sectionLabel("LOG"), s, clearBtn);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle(
                "-fx-control-inner-background: #0a0a10;" +
                        "-fx-text-fill: #7a9a7a;" +
                        "-fx-font-family: 'Courier New';" +
                        "-fx-font-size: 11px;" +
                        "-fx-border-color: #2a2a3a;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;"
        );
        VBox.setVgrow(logArea, Priority.ALWAYS);

        box.getChildren().addAll(header, logArea);
        return box;
    }

    // ─── Right Panel (Queue) ──────────────────────────────────────
    private VBox buildRightPanel() {
        VBox panel = new VBox(10);
        panel.setPrefWidth(240);
        panel.setMinWidth(200);
        panel.setPadding(new Insets(20, 16, 20, 12));
        panel.setStyle("-fx-background-color: #16161e; -fx-border-color: #2a2a3a; -fx-border-width: 0 0 0 1;");

        queueList = new ListView<>(queueItems);
        queueList.setStyle(
                "-fx-control-inner-background: #0f0f13;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: #2a2a3a;" +
                        "-fx-border-radius: 6;"
        );
        VBox.setVgrow(queueList, Priority.ALWAYS);

        // Custom cell
        queueList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(QueueItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.toString());
                    String bg = switch (item.status) {
                        case "RUNNING"   -> "#1a1a2e";
                        case "DONE"      -> "#0d1a0d";
                        case "ERROR"     -> "#1a0d0d";
                        case "CANCELLED" -> "#1a1510";
                        default          -> "#0f0f13";
                    };
                    setStyle("-fx-background-color: " + bg + "; -fx-text-fill: #aaaacc; " +
                            "-fx-font-size: 11px; -fx-font-family: 'Courier New'; -fx-padding: 8 6 8 6;");
                }
            }
        });

        Button removeBtn = styledButton("Remove Selected", "#2a1a1a", "#ff4a4a");
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.setOnAction(e -> {
            QueueItem sel = queueList.getSelectionModel().getSelectedItem();
            if (sel != null && sel.status.equals("PENDING")) queueItems.remove(sel);
        });

        Button clearDoneBtn = styledButton("Clear Done", "#1a1a2e", "#5a5a9a");
        clearDoneBtn.setMaxWidth(Double.MAX_VALUE);
        clearDoneBtn.setOnAction(e -> queueItems.removeIf(i ->
                i.status.equals("DONE") || i.status.equals("ERROR") || i.status.equals("CANCELLED")));

        panel.getChildren().addAll(
                sectionLabel("QUEUE"),
                queueList,
                removeBtn,
                clearDoneBtn
        );
        return panel;
    }

    // ─── Add to Queue ─────────────────────────────────────────────
    private void addToQueue() {
        String input = inputField.getText().trim();
        String output = outputField.getText().trim();

        if (input.isEmpty()) { showError("Please select an input file."); return; }
        if (output.isEmpty()) { showError("Please set an output path."); return; }
        if (!new File(input).exists()) { showError("Input file does not exist:\n" + input); return; }

        // Auto-extension if user didn't set one
        if (!output.contains(".")) output += ".mp4";

        queueItems.add(new QueueItem(input, output));
        log("Added to queue: " + Paths.get(input).getFileName(), "INFO");
        inputField.clear();
        outputField.clear();
    }

    // ─── Start Conversion ─────────────────────────────────────────
    private void startConversion() {
        List<QueueItem> pending = queueItems.stream()
                .filter(i -> i.status.equals("PENDING")).toList();

        if (pending.isEmpty()) {
            showError("No pending items in the queue.\nAdd files first.");
            return;
        }

        String ffmpegPath;
        try {
            ffmpegPath = getFFmpegPath();
        } catch (RuntimeException e) {
            showError("FFmpeg not found!\n\nPlace the ffmpeg binary in:\n" +
                    System.getProperty("user.dir") + "/ffmpeg/\n\n" +
                    "Or install via: brew install ffmpeg");
            return;
        }

        isCancelled = false;
        startBtn.setDisable(true);
        cancelBtn.setDisable(false);
        setStatus("Running", "#4aff88");

        for (QueueItem item : pending) {
            executor.submit(createConversionTask(item, ffmpegPath));
        }
    }

    // ─── Conversion Task ──────────────────────────────────────────
    private Task<Void> createConversionTask(QueueItem item, String ffmpeg) {
        return new Task<>() {
            @Override
            protected Void call() {
                Platform.runLater(() -> {
                    item.status = "RUNNING";
                    queueList.refresh();
                });

                try {
                    if (isCancelled) { markItem(item, "CANCELLED"); return null; }

                    double duration = getDuration(ffmpeg, item.input);
                    if (duration <= 0) {
                        log("Warning: Could not determine duration, progress may be inaccurate.", "WARN");
                        duration = 1;
                    }

                    List<String> cmd = buildCommand(ffmpeg, item.input, item.output);
                    log("Starting: " + String.join(" ", cmd), "CMD");

                    ProcessBuilder pb = new ProcessBuilder(cmd);
                    pb.redirectErrorStream(true);
                    currentProcess = pb.start();

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(currentProcess.getInputStream()));

                    Pattern timePattern = Pattern.compile("time=([0-9:.]+)");
                    String line;
                    double totalDur = duration;

                    while ((line = reader.readLine()) != null) {
                        if (isCancelled) { currentProcess.destroyForcibly(); break; }
                        final String logLine = line;

                        // Only log meaningful lines
                        if (line.contains("frame=") || line.contains("error") ||
                                line.contains("Error") || line.contains("Warning")) {
                            Platform.runLater(() -> logArea.appendText(logLine + "\n"));
                        }

                        Matcher m = timePattern.matcher(line);
                        if (m.find()) {
                            double cur = parseTime(m.group(1));
                            double prog = Math.min(cur / totalDur, 1.0);
                            Platform.runLater(() -> progressBar.setProgress(prog));
                        }
                    }

                    int exit = currentProcess.waitFor();

                    if (isCancelled) {
                        markItem(item, "CANCELLED");
                        log("Cancelled: " + Paths.get(item.input).getFileName(), "WARN");
                    } else if (exit == 0) {
                        markItem(item, "DONE");
                        Platform.runLater(() -> progressBar.setProgress(1.0));
                        log("✅ Done: " + Paths.get(item.output).getFileName(), "OK");
                    } else {
                        markItem(item, "ERROR");
                        log("❌ FFmpeg exited with code " + exit, "ERROR");
                    }

                } catch (Exception e) {
                    markItem(item, "ERROR");
                    log("❌ Exception: " + e.getMessage(), "ERROR");
                }

                // Check if all done
                Platform.runLater(() -> {
                    boolean allDone = queueItems.stream()
                            .noneMatch(i -> i.status.equals("PENDING") || i.status.equals("RUNNING"));
                    if (allDone) {
                        startBtn.setDisable(false);
                        cancelBtn.setDisable(true);
                        setStatus("Idle", "#5a5a7a");
                    }
                });

                return null;
            }
        };
    }

    private List<String> buildCommand(String ffmpeg, String input, String output) {
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpeg);
        cmd.add("-i"); cmd.add(input);

        String codec = codecBox.getValue().split(" ")[0];

        if (!codec.equals("copy")) {
            cmd.add("-vcodec"); cmd.add(codec);

            // Preset (only for x264/x265)
            if (codec.equals("libx264") || codec.equals("libx265")) {
                cmd.add("-preset"); cmd.add(presetBox.getValue());
                cmd.add("-crf"); cmd.add(String.valueOf((int) crfSlider.getValue()));
            }

            // Resolution
            String res = resolutionBox.getValue();
            if (!res.equals("Original")) {
                String[] parts = res.split(" ")[0].split("x");
                cmd.add("-vf"); cmd.add("scale=" + parts[0] + ":" + parts[1]);
            }

            cmd.add("-acodec"); cmd.add("aac");
        } else {
            cmd.add("-c"); cmd.add("copy");
        }

        cmd.add("-y"); // overwrite
        cmd.add(output);
        return cmd;
    }

    // ─── Cancel ───────────────────────────────────────────────────
    private void cancelConversion() {
        isCancelled = true;
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
        }
        log("Conversion cancelled by user.", "WARN");
        setStatus("Cancelled", "#ff9944");
        startBtn.setDisable(false);
        cancelBtn.setDisable(true);
    }

    // ─── File choosers ────────────────────────────────────────────
    private Stage primaryStage;

    private void chooseInputFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Video File");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video Files",
                        "*.mp4", "*.mkv", "*.avi", "*.mov", "*.wmv",
                        "*.flv", "*.webm", "*.m4v", "*.ts", "*.mts"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File f = fc.showOpenDialog(null);
        if (f != null) {
            inputField.setText(f.getAbsolutePath());
            // Auto-suggest output
            if (outputField.getText().isBlank()) {
                String name = f.getName().replaceAll("\\.[^.]+$", "") + "_converted.mp4";
                outputField.setText(f.getParent() + File.separator + name);
            }
        }
    }

    private void chooseSaveFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Output As");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP4", "*.mp4"),
                new FileChooser.ExtensionFilter("MKV", "*.mkv"),
                new FileChooser.ExtensionFilter("WebM", "*.webm")
        );
        File f = fc.showSaveDialog(null);
        if (f != null) outputField.setText(f.getAbsolutePath());
    }

    // ─── Drag & Drop ──────────────────────────────────────────────
    private void setupDragDrop(javafx.scene.Node node) {
        node.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });
        node.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                File dropped = db.getFiles().get(0);
                inputField.setText(dropped.getAbsolutePath());
                if (outputField.getText().isBlank()) {
                    String name = dropped.getName().replaceAll("\\.[^.]+$", "") + "_converted.mp4";
                    outputField.setText(dropped.getParent() + File.separator + name);
                }
                log("Dropped: " + dropped.getName(), "INFO");
            }
            e.setDropCompleted(true);
            e.consume();
        });
        // Visual highlight on drag-over
        node.setOnDragEntered(e -> node.setStyle(node.getStyle() +
                "-fx-border-color: #4a9eff; -fx-border-width: 1; -fx-border-radius: 6;"));
        node.setOnDragExited(e -> styleTextField((TextField) node));
    }

    // ─── FFmpeg Helpers ───────────────────────────────────────────
    private double getDuration(String ffmpeg, String input) {
        try {
            Process p = new ProcessBuilder(ffmpeg, "-i", input)
                    .redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            Pattern pat = Pattern.compile("Duration:\\s*([0-9:.]+)");
            String line;
            while ((line = r.readLine()) != null) {
                Matcher m = pat.matcher(line);
                if (m.find()) return parseTime(m.group(1));
            }
        } catch (Exception e) {
            log("Could not read duration: " + e.getMessage(), "WARN");
        }
        return -1;
    }

    private double parseTime(String t) {
        try {
            String[] p = t.split(":");
            return Double.parseDouble(p[0]) * 3600
                    + Double.parseDouble(p[1]) * 60
                    + Double.parseDouble(p[2]);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getFFmpegPath() {
        // 1. Check PATH (system install)
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version").start();
            p.waitFor(2, TimeUnit.SECONDS);
            if (p.exitValue() == 0) return "ffmpeg";
        } catch (Exception ignored) {}

        // 2. Check bundled binary
        String base = System.getProperty("user.dir");
        String unix = base + "/ffmpeg/ffmpeg";
        String win  = base + "\\ffmpeg\\ffmpeg.exe";

        if (new File(unix).exists()) return unix;
        if (new File(win).exists())  return win;

        throw new RuntimeException("FFmpeg not found in PATH or bundled directory.");
    }

    // ─── UI Helpers ───────────────────────────────────────────────
    private void markItem(QueueItem item, String status) {
        Platform.runLater(() -> {
            item.status = status;
            queueList.refresh();
        });
    }

    private void log(String msg, String level) {
        String ts = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String prefix = switch (level) {
            case "ERROR" -> "✗";
            case "OK"    -> "✓";
            case "WARN"  -> "⚠";
            case "CMD"   -> "»";
            default      -> "·";
        };
        Platform.runLater(() -> {
            logArea.appendText("[" + ts + "] " + prefix + " " + msg + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void setStatus(String text, String color) {
        Platform.runLater(() ->
                statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;")
        );
        Platform.runLater(() -> statusLabel.setText("● " + text));
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setStyle(
                "-fx-background-color: #16161e; -fx-text-fill: #e0e0f0;");
        alert.showAndWait();
    }

    private String getQualityHint(int crf) {
        if (crf <= 18) return " (Lossless)";
        if (crf <= 23) return " (High)";
        if (crf <= 28) return " (Medium)";
        if (crf <= 35) return " (Low)";
        return " (Very Low)";
    }

    // ─── Styling Helpers ──────────────────────────────────────────
    private void styleTextField(TextField tf) {
        tf.setStyle(
                "-fx-background-color: #1e1e2e;" +
                        "-fx-text-fill: #c0c0d8;" +
                        "-fx-prompt-text-fill: #4a4a6a;" +
                        "-fx-border-color: #2a2a3a;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 8 10 8 10;" +
                        "-fx-font-family: 'Courier New';" +
                        "-fx-font-size: 12px;"
        );
    }

    private void styleComboBox(ComboBox<?> cb) {
        cb.setStyle(
                "-fx-background-color: #1e1e2e;" +
                        "-fx-text-fill: #c0c0d8;" +
                        "-fx-border-color: #2a2a3a;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-size: 12px;"
        );
    }

    private Button styledButton(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + fg + ";" +
                        "-fx-border-color: " + fg + "44;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 8 14 8 14;" +
                        "-fx-font-size: 12px;" +
                        "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + fg + "22;" +
                        "-fx-text-fill: " + fg + ";" +
                        "-fx-border-color: " + fg + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 8 14 8 14;" +
                        "-fx-font-size: 12px;" +
                        "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + fg + ";" +
                        "-fx-border-color: " + fg + "44;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 8 14 8 14;" +
                        "-fx-font-size: 12px;" +
                        "-fx-cursor: hand;"
        ));
        return btn;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #4a4a6a; -fx-font-size: 10px; " +
                "-fx-font-family: 'Courier New'; -fx-font-weight: bold;");
        return l;
    }

    private Label miniLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #6a6a8a; -fx-font-size: 11px;");
        return l;
    }

    private Region gap(double w) {
        Region r = new Region();
        r.setMinWidth(w);
        r.setPrefWidth(w);
        return r;
    }

    // ─── Shutdown ─────────────────────────────────────────────────
    @Override
    public void stop() {
        isCancelled = true;
        if (currentProcess != null) currentProcess.destroyForcibly();
        executor.shutdownNow();
    }

    public static void main(String[] args) {
        launch();
    }
}