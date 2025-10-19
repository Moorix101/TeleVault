# TeleVault 🏦⌯⌲

**Turn your unlimited Telegram cloud storage into a fully-featured file manager on your Android device.**

TeleVault is an Android application that leverages the Telegram Bot API to provide a simple, effective, and free personal cloud storage solution. Upload, download, share, and manage your files securely in a private Telegram channel, all through a clean and intuitive interface.

<p float="left">
  <img src="https://github.com/Moorix101/TeleVault/blob/main/assets/1.jpg" width="150" />
  <img src="https://github.com/Moorix101/TeleVault/blob/main/assets/2.jpeg" width="150" />
  <img src="https://github.com/Moorix101/TeleVault/blob/main/assets/3.jpg" width="150" />
  <img src="https://github.com/Moorix101/TeleVault/blob/main/assets/4.jpg" width="150" />
  <img src="https://github.com/Moorix101/TeleVault/blob/main/assets/5.jpg" width="150" />
  <img src="https://github.com/Moorix101/TeleVault/blob/main/assets/6.jpg" width="150" />
</p>
  <img src="https://github.com/Moorix101/TeleVault/blob/main/assets/7.jpg" width="150" />
</p>

---

## 📖 Table of Contents

- [About The Project](#about-the-project-)
- [Key Features](#-key-features)
- [How It Works](#-how-it-works)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Step 1: Create Your Telegram Bot](#step-1-create-your-telegram-bot)
  - [Step 2: Create a Private Channel](#step-2-create-a-private-channel)
  - [Step 3: Get Your Chat ID](#step-3-get-your-chat-id)
  - [Step 4: Configure the App](#step-4-configure-the-app)
- [How to Use the App](#-how-to-use-the-app)
- [Technical Details](#-technical-details)
- [License](#-license)

---

## About The Project 🌟

The idea behind TeleVault is simple: Telegram offers unlimited cloud storage for any file type. This project provides a user-friendly Android client to interact with that storage, making it feel like a conventional cloud service.

Instead of manually uploading and searching for files in a chat, TeleVault keeps a local database of your files, allowing you to browse, sort, download, and delete them with ease. The database itself is stored as a simple text file (`televault_files.txt`) in your device's `Documents/TeleVault` folder, ensuring your file list persists even if you reinstall the app.

---

## ✨ Key Features

- **⚙️ In-App Configuration:** No need to edit code! Enter your Bot Token and Chat ID directly in the app's Settings screen and test the connection.
- **🌐 Multi-Language Support:** Fully translated and supports English, Arabic (العربية), and French (Français) with instant switching from the settings screen.
- **🖼️ Built-in Image Viewer:** Tap on any uploaded image to open it in a full-screen, zoomable viewer (`PhotoView`).
- **▶️ Video Playback:** Tap on any uploaded video to download it temporarily and play it in your device's default video player.
- **📲 Upload Files:** Pick any file from your device (up to 50MB, the Telegram Bot API limit) and upload it directly to your private cloud.
- **📤 Upload Status:** See which files are currently uploading. Cancel any upload in progress from the file menu.
- **🖼️ Media Thumbnails:** Images and videos show a thumbnail in the file list (using Glide) for easy identification.
- **📥 Download to Device:** Download any file from your cloud back to a dedicated `Documents/TeleVault/Download` folder on your device.
- **🗑️ Permanent Deletion:** Delete files from both the app list and your Telegram channel permanently.
- **🔗 Share Files Natively:** Use the Android native share sheet to send your cloud files to other apps (like WhatsApp, Gmail, etc.) without having to manually download them first.
- **🗂️ Persistent File List:** Your list of files is saved on your device in the `Documents/TeleVault` folder and survives app uninstalls.
- **🎨 Intuitive UI:** A clean, modern interface built with Material Design components.
- **📊 Storage Insights:** A dashboard shows your total file count and storage used, plus counts for specific categories like Images, Videos, PDFs, and Other.
- **🔍 Filter by Type:** Tap the category cards (Images, Videos, PDFs, Other) on the main screen to view a list of only that file type.
- **🔐 Privacy-Focused:** Your files are stored in your own private channel, controlled by your own bot. No third-party servers are involved.

---

## 🔧 How It Works

TeleVault acts as a client for the Telegram Bot API. Here’s the basic flow:

1. **Upload:** The app sends a `multipart/form-data` request to the `/sendDocument` bot API endpoint. A `TelegramUploader` class handles this.
2. **Tracking:** Upon successful upload, Telegram returns a unique `file_id` and a `message_id`. TeleVault saves this information along with the file's metadata to a local text file (`televault_files.txt`).
3. **Download:** To download a file, the app first calls the `/getFile` endpoint with the `file_id` to get a temporary download path. A `TelegramDownloader` class then downloads the file from that path.
4. **Deletion:** The app calls the `/deleteMessage` endpoint using the stored `message_id` to remove the file message from your channel, effectively deleting the file from the cloud.

---

## 🚀 Getting Started

Follow these instructions to set up the app.

### Prerequisites

- An active Telegram account.
- An Android device.

### Step 1: Create Your Telegram Bot

1. Open Telegram and search for the **`@BotFather`** user (it has a blue checkmark).
2. Send the `/newbot` command.
3. Follow the prompts:
   - Choose a name for your bot (e.g., "My Storage Bot").
   - Choose a unique username that ends in `bot` (e.g., `my_personal_storage_bot`).
4. BotFather will give you a unique API Token like: 123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11
5. **Copy and save this token securely! This is your `BOT_TOKEN`.**

### Step 2: Create a Private Channel

1. Create a **New Channel** in Telegram.
2. Give it a name (e.g., "My TeleVault Storage").
3. Set it to **Private**.
4. **Add your bot as an administrator** and grant permissions to **Post Messages** and **Delete Messages**.

### Step 3: Get Your Chat ID

1. Forward any message from your new private channel to **`@userinfobot`**.
2. The bot will reply with info — look for the **`Id:`** field.
3. It will be a negative number like `-1001234567890`.
4. **Copy and save this ID. This is your `CHAT_ID`.**

### Step 4: Configure the App

1. Install TeleVault on your Android device.
2. Open the app → tap **Settings Required** → “Go to Settings”.
3. Paste your **`BOT_TOKEN`** and **`CHAT_ID`** in the fields.
4. Tap **Check Connection** → you should see “Success”.
5. Tap **Save Settings**.
6. Done! You can now upload files.

---

## 📱 How to Use the App

1. **Grant Permissions:** On first launch, allow storage access (or “All Files Access” on newer Android versions).
2. **Upload a File:** Tap the `+` Floating Action Button → choose a file.
3. **View Media:** Tap any image/video to open it.
4. **Filter Files:** Tap a dashboard card (Images, Videos, PDFs, Other) to filter.
5. **Change Language:** Go to **Settings** → pick English, Arabic, or French.
6. **Manage Files:**
- Tap `⋮` on a file to open menu:
  - **Download** → saves to `Documents/TeleVault/Download`.
  - **Share File** → share via Android menu.
  - **Remove from Vault** → permanently deletes from Telegram.
  - **Cancel Upload** → stops current upload.

---

## 🛠️ Technical Details

- **Architecture:** Multiple activities (`MainActivity`, `SettingsActivity`, `FilteredFilesActivity`, `ImageViewerActivity`) using `RecyclerView` + `CardView`.
- **Networking:** `HttpURLConnection` for Telegram API; background threads for uploads/downloads.
- **UI Libraries:** `photoview` for zooming, `Glide` for thumbnails.
- **i18n:** Uses `AppCompatDelegate.setApplicationLocales` and `LocaleHelper`.
- **Persistence:** Simple pipe-delimited (`|`) text file database `televault_files.txt`.
- **Permissions:** Supports runtime + `MANAGE_EXTERNAL_STORAGE` for Android 11+.
- **File Sharing:** Uses `FileProvider` for secure `content://` URIs.

---

## 📄 License

Distributed under the MIT License.  
See `LICENSE` for more information.
