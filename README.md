# TeleVault 🏦⌯⌲

**Turn your unlimited Telegram cloud storage into a fully-featured file manager on your Android device.**

TeleVault is an Android application that leverages the Telegram Bot API to provide a simple, effective, and free personal cloud storage solution. Upload, download, share, and manage your files securely in a private Telegram channel, all through a clean and intuitive interface.

<p float="left">
  <img src="https://github.com/Moorix101/TeleVault/blob/main/assets/1.jpeg" width="150" />
  <img src="https://github.com/Moorix101/TeleVault/blob/main/assets/2.jpeg" width="150" />
  <img src="https://github.com/Moorix101/TeleVault/blob/main/assets/3.jpeg" width="150" />
  <img src="https://github.com/Moorix101/TeleVault/blob/main/assets/4.jpeg" width="150" />
  <img src="https://github.com/Moorix101/TeleVault/blob/main/assets/5.jpeg" width="150" />
  <img src="https://github.com/Moorix101/TeleVault/blob/main/assets/6.jpeg" width="150" />
</p>


---

## 📖 Table of Contents

* [About The Project](#about-the-project-)
* [Key Features](#-key-features)
* [How It Works](#-how-it-works)
* [Getting Started](#-getting-started)
    * [Prerequisites](#prerequisites)
    * [Step 1: Create Your Telegram Bot](#step-1-create-your-telegram-bot)
    * [Step 2: Create a Private Channel](#step-2-create-a-private-channel)
    * [Step 3: Get Your Chat ID](#step-3-get-your-chat-id)
    * [Step 4: Configure the App](#step-4-configure-the-app)
* [How to Use the App](#-how-to-use-the-app)
* [Technical Details](#-technical-details)
* [Contributing](#-contributing)
* [License](#-license)

---

## About The Project 🌟

The idea behind TeleVault is simple: Telegram offers unlimited cloud storage for any file type. This project provides a user-friendly Android client to interact with that storage, making it feel like a conventional cloud service.

Instead of manually uploading and searching for files in a chat, TeleVault keeps a local database of your files, allowing you to browse, sort, download, and delete them with ease. The database itself is stored as a simple text file (`televault_files.txt`) in your device's `Documents/TeleVault` folder, ensuring your file list persists even if you reinstall the app.

---

## ✨ Key Features

* **📲 Upload Files:** Pick any file from your device and upload it directly to your private Telegram cloud. The app handles files up to 50MB, which is the Telegram Bot API limit.
* **📊 Real-time Progress:** Monitor upload progress with a percentage indicator and a progress bar for each file.
* **📥 Download to Device:** Download any file from your cloud back to a dedicated `Documents/TeleVault/Download` folder on your device.
* **🗑️ Permanent Deletion:** Delete files from both the app list and your Telegram channel permanently by using the Telegram Bot API to delete the message.
* **🔗 Share Files Natively:** Use the Android native share sheet to send your cloud files to other apps (like WhatsApp, Gmail, etc.) without having to manually download them first.
* **🗂️ Persistent File List:** Your list of files is saved on your device in the `Documents/TeleVault` folder and survives app uninstalls, so you never lose track of your uploads.
* **🎨 Intuitive UI:** A clean, modern interface built with Material Design components like `CardView` and `RecyclerView` makes file management a breeze.
* **🔍 Sort & Organize:** Sort your files by name, date, or size to quickly find what you need.
* **💾 Storage Insights:** See a summary of your total files and the storage space they use at a glance.
* **🔐 Privacy-Focused:** Your files are stored in your own private channel, controlled by your own bot. No third-party servers are involved.

---

## 🔧 How It Works

TeleVault acts as a client for the Telegram Bot API. Here’s the basic flow:

1.  **Upload:** The app sends a `multipart/form-data` request to the `/sendDocument` bot API endpoint.
2.  **Tracking:** Upon successful upload, Telegram returns a unique `file_id` and a `message_id`. TeleVault saves this information along with the file's metadata to a local text file (`televault_files.txt`).
3.  **Download:** To download a file, the app first calls the `/getFile` endpoint with the `file_id` to get a temporary download path. It then downloads the file from that path.
4.  **Deletion:** The app calls the `/deleteMessage` endpoint using the stored `message_id` to remove the file message from your channel, effectively deleting the file from the cloud.

---

## 🚀 Getting Started

Follow these instructions to set up the project and run it on your own device.

### Prerequisites

* An active Telegram account.
* [Android Studio](https://developer.android.com/studio) (latest version recommended).
* An Android device or emulator.

### Step 1: Create Your Telegram Bot

First, you need to create a Telegram Bot that will act as your personal storage assistant.

1.  Open Telegram and search for the **`@BotFather`** user (it has a blue checkmark).
2.  Start a chat with BotFather and send the `/newbot` command.
3.  Follow the prompts:
    * Choose a name for your bot (e.g., "My Storage Bot").
    * Choose a unique username that ends in `bot` (e.g., "my_personal_storage_bot").
4.  BotFather will give you a unique API Token. It will look something like `123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11`.
5.  **Copy and save this token securely! This is your `BOT_TOKEN`**.

### Step 2: Create a Private Channel

This is where your files will be stored.

1.  In Telegram, create a **New Channel**.
2.  Give it a name (e.g., "My TeleVault Storage").
3.  Set it to be a **Private Channel**.
4.  **Add your bot (e.g., `@my_personal_storage_bot`) as an administrator.** This is crucial. Grant it permission to **Post Messages** and **Delete Messages**.

### Step 3: Get Your Chat ID

The app needs to know which chat to send files to.

1.  Forward any message from your new private channel to the **`@userinfobot`** on Telegram.
2.  The bot will reply with information about the channel. Look for the **`Id:`** field.
3.  The ID will be a negative number, for example: `-1001234567890`.
4.  **Copy and save this ID. This is your `CHAT_ID`**.

### Step 4: Configure the App

1. Clone this repository:
    ```bash
    git clone https://github.com/Moorix101/TeleVault.git
    ```

2.  Open the project in Android Studio.
3.  Navigate to `MainActivity.java` located at `app/src/main/java/com/moorixlabs/televault/MainActivity.java`.
4.  Find the following lines at the top of the file:
    ```java
    // !!! IMPORTANT: REPLACE THESE WITH YOUR OWN BOT TOKEN AND CHAT ID !!!
    private static final String BOT_TOKEN = "YOUR_BOT_TOKEN_HERE";
    private static final String CHAT_ID = "YOUR_CHAT_ID_HERE";
    ```
5.  Replace `"YOUR_BOT_TOKEN_HERE"` with the token you got from BotFather.
6.  Replace `"YOUR_CHAT_ID_HERE"` with the ID you got from `@userinfobot`.
7.  **That's it!** Build and run the app on your device or emulator.

---

## 📱 How to Use the App

1.  **Grant Permissions:** On the first launch, the app will ask for "All Files Access." This is necessary to save the file list database in the `Documents/TeleVault` folder and to download files.
2.  **Upload a File:** Tap the `+` Floating Action Button to open your device's file picker. Select a file to begin uploading.
3.  **Manage Files:**
    * Tap the `⋮` icon on any file item to open the options menu.
    * **Download:** Saves the file to your phone's `Documents/TeleVault/Download` directory.
    * **Share File:** Downloads the file to a temporary location to auto-delete later and opens the Android share menu, allowing you to send it.
    * **Remove from Vault:** Permanently deletes the file from your Telegram channel and the app.

---

## 🛠️ Technical Details

* **Architecture:** The app follows a standard Android activity structure with a `RecyclerView` and `CardView` for the UI. Asynchronous tasks like uploading, downloading, and deleting are handled on background threads.
* **Networking:** `HttpURLConnection` is used for all communication with the Telegram API.
* **Persistence:** The file list is stored in a simple pipe-delimited (`|`) text file named `televault_files.txt`. This flat-file database approach makes the data portable and independent of the app's installation status.
* **Permissions:** The app correctly handles runtime permissions for older Android versions and uses "All Files Access" (`MANAGE_EXTERNAL_STORAGE`) for Android 11 (R) and above to ensure database persistence.
* **File Sharing:** Uses Android's `FileProvider` to securely generate a `content://` URI for sharing downloaded files with other applications, following modern Android best practices.

---


## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
