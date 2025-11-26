<p align="center">
  <img src="https://github.com/user-attachments/assets/d03bcfba-ee86-4fff-9173-4c1a1b28bfb4" alt="YooDL Logo" width="150"/>
</p>

# YooDL - Universal Media Downloader

A powerful Android application built with **Kotlin** and **Jetpack Compose** that allows users to download media from **YouTube, Instagram, Facebook, TikTok** and **1000+ websites** with ease.

## 🌟 Features at a Glance

[![GitHub Downloads](https://img.shields.io/github/downloads/YooAshu/YooDL/total?style=flat-square&logo=github)](https://github.com/YooAshu/YooDL/releases)
[![GitHub Repo Stars](https://img.shields.io/github/stars/YooAshu/YooDL?style=flat-square&logo=github)](https://github.com/YooAshu/YooDL)

## 📲 Quick Download

[⬇️ **DOWNLOAD LATEST APK**](https://github.com/YooAshu/YooDL/releases/latest) 
- **v1.0.0** | Latest Release | Direct from GitHub

---

## 🎯 Core Features

### Download Capabilities
- 🎬 **YouTube** - Videos, playlists, shorts, channels
- 📸 **Instagram** - Posts, reels, stories, IGTV
- 👥 **Facebook** - Videos, live streams
- 🎵 **TikTok** - Videos and sounds
- 🌐 **1000+ Websites** - Reddit, Twitter, Vimeo, Dailymotion, and more
- 📚 Playlist and batch downloads
- 🎨 Multiple quality options
- 🎧 Audio-only extraction

### App Features
- ✅ **Play Directly** - Click on File to play with Your local player
- ✅ **Directly Download** - Directly share from youtube or other sites to YooDL app to download
- ✅ **Delete Downloads** - Manage storage easily
- ✅ **Queue Management** - Organize batch downloads
- ✅ **Real-time Progress** - Live status tracking
- ✅ **Beautiful UI** - Modern Jetpack Compose design
- ✅ **Material Design 3** - Latest material components
- ✅ **Offline Support** - Download and play locally

### Supported Sites

**Video Platforms:**
YouTube, Instagram, Facebook, TikTok, Vimeo, Dailymotion, Twitter/X, Reddit, Twitch, Mixer, and more...

**For complete list of 1000+ supported websites**, visit: [**YT-DLP Supported Sites**](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md)

---

## 📸 Screebshots

### Home Screen 
- URL input with search functionality
- Video information display
- Playlist support
- directly share from apps like instagram and youtube and facebook to download directly
<img src="https://github.com/user-attachments/assets/6baab423-57c6-45ab-828e-046cfb14623d" alt="Screenshot 1" style="width: 200px; height: auto;">
<img src="https://github.com/user-attachments/assets/812aab56-c4ad-4bd8-9a2d-d9b986e8cf5a" alt="Screenshot 1" style="width: 200px; height: auto;">

### Downloads Screen 
- Queue management
- Real-time progress tracking
- Download status indicators
<img src="https://github.com/user-attachments/assets/e919e3bb-59a3-4876-967d-ac26b3e647f0" alt="Screenshot 1" style="width: 200px; height: auto;">

### Download Options 
- Quality selection
- Format choices
- Audio-only downloads
<img src="https://github.com/user-attachments/assets/9a43e55b-afa8-43d2-878a-b83354166f27" alt="Screenshot 1" style="width: 200px; height: auto;">

## 📥 Installation & Setup

### Prerequisites

- Android 8.0 (API 26) or higher
- 50MB free storage
- Internet connection
- Android Studio Otter | 2025.2.1 (for development)

### Download APK

**Latest Release:** [YooDL v1.0.0](https://github.com/YooAshu/YooDL/releases/latest)

1. Download `YooDL-v1.0.0.apk` from releases
2. Transfer to your Android device
3. Open file manager → Tap APK → Install
4. Grant required permissions
5. Launch and start downloading!

### Build from Source

```bash
# Clone repository
git clone https://github.com/YooAshu/YooDL.git
cd YooDL

# Build APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

---

## 🔑 Configuration

### API Setup

Add your YouTube API key to `local.properties`:

```ini
# local.properties
YOUTUBE_API_KEY=YOUR_YOUTUBE_API_KEY_HERE
```

**Get YouTube API Key:**
1. Visit [Google Cloud Console](https://console.cloud.google.com/)
2. Create new project
3. Enable YouTube Data API v3
4. Create API key (Credentials)
5. Add key to `local.properties`

---

## 🛠️ Technology Stack

### Architecture
- **Pattern**: MVVM (Model-View-ViewModel)
- **State Management**: StateFlow, MutableStateFlow
- **UI Framework**: Jetpack Compose

### Libraries & Dependencies
- **Jetpack Compose** - Modern declarative UI
- **Room Database** - Local data persistence
- **Retrofit & OkHttp** - Network requests
- **Hilt** - Dependency injection
- **Coroutines** - Asynchronous operations
- **yt-dlp** - Media extraction engine
- **YouTubeDL-Android** - YT-DLP wrapper
- **Material Design 3** - UI components

### External Dependencies
- [**yt-dlp**](https://github.com/yt-dlp/yt-dlp) - Universal media downloader
- [**YouTubeDL-Android**](https://github.com/yausername/youtubedl-android) - Android wrapper

---

## 📁 Project Structure

```
app/src/main/java/com/example/yoodl/
├── MainActivity.kt              # Main activity entry point
├── MyApp.kt                     # Application configuration
├── data/
│   ├── api/                     # API services
│   │   ├── APIService.kt
│   │   └── RetroFitInstance.kt
│   ├── database/                # Room database configuration
│   │   ├── DataBaseConfig.kt
│   │   ├── converters/
│   │   ├── dao/
│   │   └── entities/
│   ├── models/                  # Data models
│   │   ├── DownloadItemModel.kt
│   │   └── YTModel.kt
│   └── repository/              # Repository pattern
│       └── DownloadRepositoryV2.kt
├── di/                          # Dependency Injection (Hilt)
│   ├── DatabaseModule.kt
│   └── DownloadRepositoryModule.kt
├── ui/
│   ├── navigation/              # Navigation graphs
│   │   ├── BottomNav.kt
│   │   └── MainNavGraph.kt
│   ├── pages/                   # UI Screens
│   │   ├── downloads/
│   │   │   ├── DownloadPageScreen.kt
│   │   │   └── DownloadPageVM2.kt
│   │   └── homepage/
│   │       ├── HomePageScreen.kt
│   │       ├── HomePageVM.kt
│   │       └── components/
│   ├── theme/                   # UI Theme
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── utils/                   # Utility functions
│       └── youtube/
│           └── FileOpener.kt

```

---



## 🚀 Usage Guide

### Basic Download
1. **Paste URL** - Enter any supported website link
2. **Select Quality** - Choose video/audio quality
3. **Choose Format** - MP4, MKV, MP3, WAV, etc.
4. **Download** - Add to queue and monitor progress



### Managing Downloads
- ⏸️ **Pause** - Pause active downloads
- ▶️ **Resume** - Continue paused downloads
- 🗑️ **Delete** - Remove downloaded files
- ▶️ **Play** - Open in built-in player
- 📂 **Share** - Share with other apps

---

## 🤝 Contributing

### How to Contribute

1. **Fork Repository**
   ```bash
   git clone https://github.com/YooAshu/YooDL.git
   ```

2. **Create Feature Branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```

3. **Make Changes**
   - Follow Kotlin style guide
   - Add meaningful comments
   - Test thoroughly

4. **Commit Changes**
   ```bash
   git commit -m "feat: Add amazing feature"
   git commit -m "fix: Resolve download issue"
   git commit -m "docs: Update README"
   ```

5. **Push to Fork**
   ```bash
   git push origin feature/amazing-feature
   ```

6. **Open Pull Request**
   - Describe changes clearly
   - Add screenshots if UI changes
   - Reference related issues

### Code Guidelines

- **Language**: Kotlin (primary), Java (legacy)
- **Style**: [Kotlin Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Naming**: camelCase for variables, PascalCase for classes
- **Comments**: Add for complex logic
- **Tests**: Unit tests for business logic

### Areas for Contribution

- 🐛 **Bug Fixes** - Report and fix issues
- ✨ **Features** - New download sources, UI improvements
- 📚 **Documentation** - Improve guides and comments
- 🧪 **Testing** - Add unit and integration tests
- 🌐 **Translation** - Support new languages
- 🎨 **UI/UX** - Design improvements

---

## 🐛 Issue Reporting

Found a bug? [**Open an Issue**](https://github.com/YooAshu/YooDL/issues)

**Include:**
- Clear description
- Steps to reproduce
- Screenshots/videos
- Device info (Android version, device model)
- App version

---

## 📊 Project Stats

- **Language**: Kotlin, Java
- **Min SDK**: API 26 (Android 8.0)
- **Target SDK**: API 34+ (Android 14+)
- **Architecture**: MVVM + Repository Pattern
- **Database**: Room
- **UI Framework**: Jetpack Compose

---

## 📚 Resources & Links

| Resource | Link |
|----------|------|
| **GitHub Releases** | [Download APK](https://github.com/YooAshu/YooDL/releases) |
| **yt-dlp Project** | [yt-dlp/yt-dlp](https://github.com/yt-dlp/yt-dlp) |
| **YT-DLP Wrapper** | [yausername/youtubedl-android](https://github.com/yausername/youtubedl-android) |
| **Issue Tracker** | [GitHub Issues](https://github.com/YooAshu/YooDL/issues) |
| **Report Bug** | [New Issue](https://github.com/YooAshu/YooDL/issues/new) |

---

## 📄 License

Licensed under **MIT License** - See [LICENSE](LICENSE) file

---

## 🙏 Acknowledgments

- **yt-dlp team** - Universal media downloader
- **YouTubeDL-Android** - Android integration
- **Jetpack Compose** team - Modern UI toolkit
- **Contributors** - All amazing developers

---

## ⭐ Support

If YooDL helped you, please consider:
- ⭐ **Star** this repository
- 🔄 **Share** with friends
- 🐛 **Report bugs** and suggest features
- 💝 **Contribute** code or translations

---

**Made with ❤️ using Kotlin & Jetpack Compose**

![YooDL Banner](https://via.placeholder.com/1200x400/1f6feb/ffffff?text=YooDL+-+Universal+Media+Downloader)
