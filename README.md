# ⚡ FFmpeg Pro Tool

A lightweight, modern **JavaFX desktop video conversion application powered by FFmpeg**.

FFmpeg Pro provides an easy-to-use graphical interface for converting videos without requiring users to work directly with complex FFmpeg commands. It supports multiple codecs, encoding presets, resolutions, CRF quality control, batch conversion through a queue, real-time progress tracking, cancellation, drag-and-drop input, and detailed conversion logs.

---

## ✨ Features

### 🎬 Video Conversion

* Convert videos using FFmpeg
* Supports multiple common video formats
* Automatic FFmpeg detection
* Supports system-installed FFmpeg
* Supports a bundled FFmpeg binary
* Automatically generates an output filename
* Automatically adds `.mp4` when no output extension is specified
* Existing output files can be overwritten

### 📥 Input & Output

* File browser for selecting input videos
* File browser for selecting output location
* Drag-and-drop video files into the application
* Automatic output filename suggestion
* Input file existence validation
* Output format selection

Supported input formats include:

* MP4
* MKV
* AVI
* MOV
* WMV
* FLV
* WebM
* M4V
* TS
* MTS

Supported save formats:

* MP4
* MKV
* WebM

---

## 🎞️ Encoding Options

### Video Codecs

The application currently provides:

| Codec        | Description                |
| ------------ | -------------------------- |
| `libx264`    | H.264 / AVC                |
| `libx265`    | H.265 / HEVC               |
| `libvpx-vp9` | VP9                        |
| `copy`       | No video/audio re-encoding |

### Encoding Presets

For H.264 and H.265 encoding:

* Ultrafast
* Superfast
* Veryfast
* Faster
* Fast
* Medium
* Slow
* Slower
* Veryslow

The default preset is:

```text
medium
```

### CRF Quality Control

The application provides a CRF slider from:

```text
0 - 51
```

Default:

```text
CRF 23
```

Quality hints are displayed automatically:

|   CRF | Quality Hint |
| ----: | ------------ |
|  0–18 | Lossless     |
| 19–23 | High         |
| 24–28 | Medium       |
| 29–35 | Low          |
| 36–51 | Very Low     |

---

## 📐 Resolution Scaling

The following resolution options are available:

* Original
* 3840×2160 — 4K
* 1920×1080 — 1080p
* 1280×720 — 720p
* 854×480 — 480p
* 640×360 — 360p

When a resolution is selected, FFmpeg applies the appropriate scaling filter.

Example:

```text
-vf scale=1920:1080
```

---

## 🚀 Conversion Queue

FFmpeg Pro includes a conversion queue for processing multiple files.

### Queue statuses

Each queued item can have one of the following states:

```text
PENDING
RUNNING
DONE
ERROR
CANCELLED
```

The UI displays visual status indicators:

```text
🕐 PENDING
⏳ RUNNING
✅ DONE
❌ ERROR
🚫 CANCELLED
```

### Queue operations

* Add files to queue
* Remove selected pending item
* Clear completed items
* Clear failed items
* Clear cancelled items
* Start all pending conversions

---

## ⚡ Parallel Processing

Conversion jobs are executed using a Java `ExecutorService`.

The application creates a fixed thread pool with **2 worker threads**, allowing multiple queued conversions to be processed concurrently.

```java
Executors.newFixedThreadPool(2);
```

This makes it possible to process multiple queue items without blocking the JavaFX UI.

---

## 📊 Real-Time Progress

The application calculates conversion progress from FFmpeg's reported processing time.

It:

1. Detects the input video's duration.
2. Starts the FFmpeg process.
3. Reads FFmpeg output.
4. Extracts the current processing timestamp.
5. Calculates the percentage.
6. Updates the JavaFX progress bar.

Example:

```text
0%
25%
50%
75%
100%
```

The progress display supports decimal percentages such as:

```text
73.4%
```

---

## 🛑 Conversion Cancellation

The conversion can be cancelled while FFmpeg is running.

When the user presses **Cancel**:

* The cancellation flag is set.
* The active FFmpeg process is forcibly terminated.
* The queue item is marked `CANCELLED`.
* The UI status changes to `Cancelled`.
* The Start button becomes available again.

---

## 🖱️ Drag & Drop

Video files can be dragged directly into the input field.

The application:

* Detects dropped files
* Sets the input path automatically
* Generates an output filename
* Logs the dropped filename
* Provides visual feedback while dragging

Example:

```text
movie.mkv
    ↓
movie_converted.mp4
```

---

## 📝 Conversion Logs

The application includes a built-in log console.

Logs include:

* Application status
* Added queue items
* Dropped files
* FFmpeg commands
* Warnings
* Errors
* Completed conversions
* Cancelled conversions

Log entries include timestamps:

```text
[20:15:32] · Added to queue: video.mkv
[20:15:33] » Starting: ffmpeg -i video.mkv ...
[20:16:10] ✓ Done: video.mp4
```

The log area can also be cleared using the **Clear** button.

---

## 🔎 Automatic FFmpeg Detection

The application looks for FFmpeg in two locations.

### 1. System PATH

It first attempts:

```bash
ffmpeg -version
```

If FFmpeg is installed globally, it uses:

```text
ffmpeg
```

### 2. Bundled FFmpeg

If FFmpeg isn't available through PATH, the application checks:

```text
./ffmpeg/ffmpeg
```

on Unix-like systems and:

```text
.\ffmpeg\ffmpeg.exe
```

on Windows.

If FFmpeg cannot be found, the application displays an installation/location error.

---

## 🖥️ User Interface

The application uses **JavaFX** and features a dark desktop interface.

### Main UI sections

```text
┌─────────────────────────────────────────────────────────┐
│ ⚡ FFmpeg Pro                         ● Idle             │
├───────────────────────────────────────┬─────────────────┤
│ INPUT / OUTPUT                        │ QUEUE           │
│                                       │                 │
│ [Input File              ] [Browse]   │ 🕐 video1.mp4   │
│ [Output File             ] [Save As]  │ ⏳ video2.mkv   │
│                         [+ Add Queue]  │ ✅ video3.avi   │
│                                       │                 │
│ ENCODING SETTINGS                     │ [Remove]        │
│ Codec       [H.264              ]     │ [Clear Done]    │
│ Preset      [medium             ]     │                 │
│ Resolution  [Original           ]     │                 │
│                                       │                 │
│ Quality (CRF)                         │                 │
│ ───────────────●──── CRF: 23 (High) │                 │
│                                       │                 │
│ ████████████████████ 72.5%            │                 │
│                                       │                 │
│ [▶ Start Conversion] [■ Cancel]       │                 │
│                                       │                 │
│ LOG                                   │                 │
│ [20:10:20] · Ready...                │                 │
│ [20:10:25] » Starting...             │                 │
└───────────────────────────────────────┴─────────────────┘
```

---

## 🛠️ Technology Stack

| Technology           | Purpose                          |
| -------------------- | -------------------------------- |
| Java                 | Application programming language |
| JavaFX               | Desktop graphical user interface |
| FFmpeg               | Video/audio processing engine    |
| Java ExecutorService | Background conversion tasks      |
| ProcessBuilder       | FFmpeg process execution         |
| Regular Expressions  | FFmpeg output parsing            |
| NIO / File API       | File and path management         |

---

## 📦 Requirements

### Java

A modern Java version supporting the JavaFX application is required.

Recommended:

```text
Java 17+
```

### JavaFX

The application requires JavaFX modules, including:

* `javafx.application`
* `javafx.collections`
* `javafx.concurrent`
* `javafx.geometry`
* `javafx.scene`
* `javafx.stage`

The source uses JavaFX components extensively for the UI and background task handling.

### FFmpeg

FFmpeg must either be:

1. Installed and available in the system PATH, or
2. Placed inside the application's `ffmpeg` directory.

---

## 📁 Suggested Project Structure

```text
ffmpeg-pro/
│
├── src/
│   └── FFmpegProApp.java
│
├── ffmpeg/
│   ├── ffmpeg
│   └── ffmpeg.exe
│
├── README.md
├── LICENSE
└── .gitignore
```

> **Note:** The repository should generally not commit platform-specific FFmpeg binaries unless you have the appropriate redistribution rights. Consider providing installation instructions or downloading the appropriate FFmpeg build separately.

---

## ▶️ Running the Application

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/ffmpeg-pro.git
cd ffmpeg-pro
```

### 2. Install FFmpeg

#### macOS

Using Homebrew:

```bash
brew install ffmpeg
```

#### Ubuntu / Debian

```bash
sudo apt update
sudo apt install ffmpeg
```

#### Windows

Install FFmpeg and make sure the `ffmpeg` executable is available through PATH.

Alternatively, place the executable in:

```text
ffmpeg/ffmpeg.exe
```

### 3. Build and run

Configure JavaFX for your environment and run:

```bash
javac --module-path /path/to/javafx/lib \
      --add-modules javafx.controls \
      src/FFmpegProApp.java
```

Then:

```bash
java --module-path /path/to/javafx/lib \
     --add-modules javafx.controls \
     -cp src FFmpegProApp
```

> The exact JavaFX commands depend on your operating system and JavaFX installation.

---

## 🔧 FFmpeg Command Generation

The application dynamically builds the FFmpeg command based on the selected settings.

For example, an H.264 conversion can generate a command equivalent to:

```bash
ffmpeg -i input.mkv \
  -vcodec libx264 \
  -preset medium \
  -crf 23 \
  -acodec aac \
  -y output.mp4
```

With resolution scaling:

```bash
ffmpeg -i input.mkv \
  -vcodec libx264 \
  -preset medium \
  -crf 23 \
  -vf scale=1920:1080 \
  -acodec aac \
  -y output.mp4
```

For stream copying:

```bash
ffmpeg -i input.mkv \
  -c copy \
  -y output.mkv
```

The command-building logic is implemented directly in the application.

---

## 🔐 File Safety

The application validates that:

* An input file has been selected.
* An output path has been provided.
* The input file exists before adding it to the queue.

Example validation:

```text
Input missing
     ↓
Show error
     ↓
Do not add to queue
```

---

## 🎯 Design Goals

FFmpeg Pro is designed around several principles:

* **Simple:** No need to manually write FFmpeg commands.
* **Fast:** Uses FFmpeg directly instead of reimplementing media processing.
* **Lightweight:** Native desktop application with minimal dependencies.
* **Transparent:** Generated FFmpeg commands and important processing output are visible in the log.
* **Batch-friendly:** Multiple files can be queued.
* **User-friendly:** Drag-and-drop and graphical configuration.
* **Non-blocking UI:** Video processing runs outside the JavaFX application thread.

---

## 🚧 Current Limitations

The current implementation intentionally keeps the feature set focused.

Current limitations include:

* Two conversion workers are used concurrently.
* Progress depends on FFmpeg duration/time output.
* Audio encoding is currently AAC when re-encoding.
* Limited codec configuration beyond the provided controls.
* No thumbnail/video preview.
* No persistent queue storage.
* No pause/resume conversion.
* No hardware-accelerated encoding configuration in the UI.
* No subtitle-specific controls.
* No advanced audio controls.
* No custom FFmpeg argument editor.

---

## 🔮 Possible Future Features

Potential future improvements:

### 🎥 Media Features

* Video preview
* Thumbnail generation
* Media information panel
* Bitrate display
* FPS selection
* Frame-rate conversion
* Aspect-ratio controls
* Rotation
* Cropping
* Deinterlacing

### ⚡ Hardware Acceleration

Support configurable hardware encoders such as:

```text
NVENC
Quick Sync
VideoToolbox
AMF
VAAPI
```

### 🔊 Audio

* Audio codec selection
* Audio bitrate
* Sample rate
* Channel configuration
* Audio stream selection
* Audio-only extraction

### 💬 Subtitles

* Subtitle stream selection
* Subtitle extraction
* Subtitle embedding
* Subtitle burn-in

### 📦 Queue Improvements

* Reorder queue
* Pause queue
* Pause individual job
* Retry failed jobs
* Import/export queue
* Persistent queue
* Maximum concurrent job configuration

### 🧰 Advanced FFmpeg Controls

* Custom FFmpeg arguments
* Video bitrate
* Audio bitrate
* GOP size
* FPS
* Profile
* Level
* Pixel format
* Tune
* Two-pass encoding

### 📊 Better Progress Information

* Remaining time
* Encoding speed
* Current FPS
* Output size
* Estimated final size
* Current bitrate

---

## 🐛 Error Handling

The application handles several failure scenarios:

* Missing input file
* Missing output path
* Missing FFmpeg
* Unable to determine video duration
* FFmpeg process errors
* Non-zero FFmpeg exit codes
* Java exceptions during processing
* User cancellation

FFmpeg errors and exceptions are reported through the application's log system.

---

## 🧹 Application Shutdown

When the application closes:

* Active conversion cancellation is requested.
* The current FFmpeg process is terminated.
* The executor service is shut down.

This prevents background FFmpeg processes from being left running after application exit.

---

## 📜 License

Choose an appropriate license for your project.

For example:

```text
MIT License
```

Add a `LICENSE` file to the repository if you choose MIT or another open-source license.

---

## 🤝 Contributing

Contributions are welcome.

### Recommended workflow

```bash
# Fork the repository

# Create a feature branch
git checkout -b feature/my-feature

# Make your changes

# Commit
git commit -m "Add my feature"

# Push
git push origin feature/my-feature

# Open a Pull Request
```

Before submitting a PR, make sure the application still:

* Starts correctly
* Detects FFmpeg
* Adds files to the queue
* Converts videos
* Updates progress
* Handles cancellation
* Reports errors correctly

---

## ⭐ Why FFmpeg Pro?

Instead of remembering complicated FFmpeg commands such as:

```bash
ffmpeg -i input.mkv -c:v libx264 -preset medium -crf 23 ...
```

you can configure the conversion visually:

```text
Codec       → H.264
Preset      → Medium
Resolution  → 1080p
Quality     → CRF 23
             ↓
       Start Conversion
```

**A simple graphical interface for powerful FFmpeg video processing.**

---

## 📸 Screenshots

Add application screenshots here:

```markdown
![FFmpeg Pro Main Interface](screenshots/main.png)
```

Recommended screenshots:

* Main application
* Encoding settings
* Queue with multiple files
* Conversion progress
* Completed conversion
* Error/log output

---

## 📋 Project Status

**Status:** 🟢 Working / Functional

Current implementation includes the core video conversion workflow, graphical configuration, queue processing, progress monitoring, cancellation, FFmpeg detection, logging, and drag-and-drop support.

---

## 👨‍💻 Author

**Your Name**

GitHub: `@YOUR_USERNAME`

---

## ⭐ Support

If you find this project useful, consider giving the repository a ⭐ on GitHub.

---

**Built with ☕ Java + ⚡ JavaFX + 🎬 FFmpeg**
