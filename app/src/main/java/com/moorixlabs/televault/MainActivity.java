package com.moorixlabs.televault;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String ZARCHIVER_PACKAGE_NAME = "ru.zdevs.zarchiver";

    // !!! IMPORTANT: REPLACE THESE WITH YOUR OWN BOT TOKEN AND CHAT ID !!!
    private static final String BOT_TOKEN = "YOUR_BOT_TOKEN_HERE";
    private static final String CHAT_ID = "YOUR_CHAT_ID_HERE";

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String FILES_DATABASE = "televault_files.txt";
    private static final String APP_FOLDER = "TeleVault";

    private RecyclerView recyclerViewFiles;
    private LinearLayout emptyStateLayout;
    private TextView tvStorageInfo;
    private FloatingActionButton fabAddFile;
    private ImageButton btnSort;

    private FileAdapter fileAdapter;
    private List<CloudFile> cloudFiles;

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> allFilesPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupActivityLaunchers();
        setupClickListeners();

        // Check permissions and initialize
        checkAndRequestPermissions();
    }

    private void initializeViews() {
        recyclerViewFiles = findViewById(R.id.recyclerViewFiles);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        tvStorageInfo = findViewById(R.id.tvStorageInfo);
        fabAddFile = findViewById(R.id.fabAddFile);
        btnSort = findViewById(R.id.btnSort);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupRecyclerView() {
        cloudFiles = new ArrayList<>();
// Inside setupRecyclerView()
// Inside setupRecyclerView()
// Inside setupRecyclerView()
        fileAdapter = new FileAdapter(this, cloudFiles, new FileAdapter.FileActionListener() {
            @Override
            public void onDownloadFile(CloudFile file) {
                downloadFromTelegram(file);
            }

            @Override
            public void onShareFileAction(CloudFile file) {
                // This is our new method to implement
                shareDownloadedFile(file);
            }

            @Override // <-- This was the missing line
            public void onDeleteFile(CloudFile file, int position) {
                // We will call our new, powerful delete method here
                deleteFileFromVault(file, position);
            }
        });

        recyclerViewFiles.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFiles.setAdapter(fileAdapter);

        // Load files after permission check
        if (hasStoragePermission()) {
            loadFilesFromDatabase();
            updateUI();
        }
    }

    private void setupActivityLaunchers() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            handleSelectedFile(fileUri);
                        }
                    }
                }
        );

        allFilesPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                            initializeAppFolder();
                            loadFilesFromDatabase();
                            updateUI();
                        } else {
                            showPermissionDeniedDialog();
                        }
                    }
                }
        );
    }

    private void setupClickListeners() {
        fabAddFile.setOnClickListener(v -> {
            if (hasStoragePermission()) {
                openFilePicker();
            } else {
                checkAndRequestPermissions();
            }
        });

        btnSort.setOnClickListener(v -> showSortDialog());
    }

    // ==================== STORAGE LOCATION ====================

    /**
     * Get the app folder in Documents directory
     */
    private File getAppFolder() {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File appFolder = new File(documentsDir, APP_FOLDER);
        return appFolder;
    }

    /**
     * Get the database file location
     */
    private File getDatabaseFile() {
        return new File(getAppFolder(), FILES_DATABASE);
    }

    /**
     * Initialize app folder if it doesn't exist
     */
    private void initializeAppFolder() {
        try {
            File appFolder = getAppFolder();
            if (!appFolder.exists()) {
                boolean created = appFolder.mkdirs();
                if (created) {
                    Toast.makeText(this,
                            "Database location: " + appFolder.getAbsolutePath(),
                            Toast.LENGTH_LONG).show();
                }
            }

            // Also create Download folder
            File downloadFolder = new File(appFolder, "Download");
            if (!downloadFolder.exists()) {
                downloadFolder.mkdirs();
            }
        } catch (Exception e) {
            Toast.makeText(this,
                    "Error creating app folder: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== PERMISSION HANDLING ====================

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showAllFilesAccessDialog();
            } else {
                initializeAppFolder();
                loadFilesFromDatabase();
                updateUI();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasStoragePermission()) {
                requestStoragePermissions();
            } else {
                initializeAppFolder();
                loadFilesFromDatabase();
                updateUI();
            }
        } else {
            initializeAppFolder();
            loadFilesFromDatabase();
            updateUI();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void showAllFilesAccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Storage Permission Required")
                .setMessage("This app needs access to storage to:\n" +
                        "• Upload files to Telegram\n" +
                        "• Download files from Telegram\n" +
                        "• Save database in Documents/TeleVault\n" +
                        "• Keep your file list even after reinstalling\n\n" +
                        "Please grant 'All Files Access' permission.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        allFilesPermissionLauncher.launch(intent);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) ->
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show())
                .setCancelable(false)
                .show();
    }

    private void requestStoragePermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                initializeAppFolder();
                loadFilesFromDatabase();
                updateUI();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("Storage permission is required to:\n" +
                        "• Save your file database\n" +
                        "• Upload/download files\n\n" +
                        "Please enable it in app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==================== FILE HANDLING ====================

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "Select File"));
        } catch (Exception e) {
            Toast.makeText(this, "Error opening file picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSelectedFile(Uri fileUri) {
        try {
            String fileName = FileUtils.getFileName(this, fileUri);
            long fileSize = FileUtils.getFileSize(this, fileUri);

            if (fileSize == 0) {
                Toast.makeText(this, "Cannot upload: File is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check file size limit (Telegram limit is 50MB for bots)
            if (fileSize > 50 * 1024 * 1024) {
                Toast.makeText(this, "File too large! Maximum size is 50MB", Toast.LENGTH_LONG).show();
                return;
            }

            String filePath = fileUri.toString();
            String fileId = UUID.randomUUID().toString();

            CloudFile cloudFile = new CloudFile(
                    fileId,
                    fileName,
                    fileSize,
                    System.currentTimeMillis(),
                    filePath,
                    false
            );

            cloudFiles.add(0, cloudFile);
            fileAdapter.notifyItemInserted(0);
            recyclerViewFiles.smoothScrollToPosition(0);
            updateUI();

            // Save to database immediately
            saveFileToDatabase(cloudFile);

            Snackbar.make(findViewById(android.R.id.content),
                    "Uploading: " + fileName,
                    Snackbar.LENGTH_SHORT).show();

            // Start upload to Telegram
            uploadToTelegram(cloudFile, fileUri);

        } catch (Exception e) {
            Toast.makeText(this, "Error adding file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadToTelegram(CloudFile cloudFile, Uri fileUri) {
        new Thread(() -> {
            TelegramUploader uploader = new TelegramUploader(
                    getApplicationContext(),
                    BOT_TOKEN,
                    CHAT_ID,
                    fileUri,
                    cloudFile.getName(),
                    new TelegramUploader.UploadCallback() {
                        @Override
                        public void onUploadProgress(int progress) {
                            runOnUiThread(() -> {
                                cloudFile.setUploadProgress(progress);
                                int position = cloudFiles.indexOf(cloudFile);
                                if (position != -1) {
                                    fileAdapter.notifyItemChanged(position);
                                }
                            });
                        }

                        @Override
                        public void onUploadSuccess(String fileId, String messageId) {
                            runOnUiThread(() -> {
                                cloudFile.setUploaded(true);
                                cloudFile.setUploadProgress(100);
                                cloudFile.setFileId(fileId);
                                cloudFile.setMessageId(messageId);

                                // Update database
                                updateFileInDatabase(cloudFile);

                                int position = cloudFiles.indexOf(cloudFile);
                                if (position != -1) {
                                    fileAdapter.notifyItemChanged(position);
                                }

                                Snackbar.make(findViewById(android.R.id.content),
                                        "✓ " + cloudFile.getName() + " uploaded successfully!",
                                        Snackbar.LENGTH_LONG).show();
                            });
                        }

                        @Override
                        public void onUploadFailed(String error) {
                            runOnUiThread(() -> {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Upload Failed")
                                        .setMessage("File: " + cloudFile.getName() + "\n\nError: " + error + "\n\nThe file will be removed from the list.")
                                        .setPositiveButton("OK", (dialog, which) -> {
                                            // Remove failed upload from list and database
                                            int position = cloudFiles.indexOf(cloudFile);
                                            if (position != -1) {
                                                cloudFiles.remove(position);
                                                fileAdapter.notifyItemRemoved(position);
                                                deleteFileFromDatabase(cloudFile);
                                                updateUI();
                                            }
                                        })
                                        .show();
                            });
                        }
                    }
            );
            uploader.upload();
        }).start();
    }

    private void downloadFromTelegram(CloudFile file) {
        if (!file.canDownload()) {
            Toast.makeText(this, "File not available for download", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasStoragePermission()) {
            Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
            checkAndRequestPermissions();
            return;
        }

        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Downloading")
                .setMessage("Downloading " + file.getName() + "...\n\nThis may take a moment.")
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            TelegramDownloader downloader = new TelegramDownloader(
                    getApplicationContext(),
                    BOT_TOKEN,
                    file.getFileId(),
                    file.getName(),
                    new TelegramDownloader.DownloadCallback() {
                        @Override
                        public void onDownloadProgress(int progress) {
                            runOnUiThread(() -> {
                                progressDialog.setMessage("Downloading " + file.getName() + "...\n\n" + progress + "%");
                            });
                        }

                        @Override
                        public void onDownloadSuccess(Uri fileUri) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();

                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Download Complete")
                                        .setMessage(file.getName() + " has been downloaded to:\n\nDocuments/TeleVault/Download")
                                        .setPositiveButton("Open File", (dialog, which) -> {
                                            openDownloadedFile(fileUri, file.getName());
                                        })
                                        .setNegativeButton("Open Folder", (dialog, which) -> {
                                            openDownloadFolder();
                                        })
                                        .setNeutralButton("Close", null)
                                        .show();
                            });
                        }

                        @Override
                        public void onDownloadFailed(String error) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();

                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Download Failed")
                                        .setMessage("Could not download " + file.getName() + "\n\nError: " + error)
                                        .setPositiveButton("OK", null)
                                        .show();
                            });
                        }
                    }
            );
            downloader.download();
        }).start();
    }

    private void openDownloadedFile(Uri fileUri, String fileName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);

            // For Android 7.0+ use FileProvider for file:// URIs
            if (fileUri.getScheme() != null && fileUri.getScheme().equals("file")) {
                File file = new File(fileUri.getPath());
                Uri contentUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", file);
                intent.setDataAndType(contentUri, FileUtils.getMimeType(fileName));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(fileUri, FileUtils.getMimeType(fileName));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            startActivity(Intent.createChooser(intent, "Open file with"));
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDownloadFolder() {
        // 1. Get the path to your download folder (no changes here)
        File downloadFolder = new File(getAppFolder(), "Download");
        if (!downloadFolder.exists()) {
            Toast.makeText(this, "Download folder not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Create a content URI using FileProvider (no changes here)
        Uri folderUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider",
                downloadFolder);

        // --- NEW LOGIC ---

        // 3. Create an intent that SPECIFICALLY targets ZArchiver
        Intent zarchiverIntent = new Intent(Intent.ACTION_VIEW);
        zarchiverIntent.setData(folderUri);
        zarchiverIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        zarchiverIntent.setPackage(ZARCHIVER_PACKAGE_NAME); // This line targets ZArchiver

        // 4. Check if ZArchiver is installed and can open the folder
        if (zarchiverIntent.resolveActivity(getPackageManager()) != null) {
            // Success! ZArchiver is available, so launch it directly.
            startActivity(zarchiverIntent);
        } else {
            // Fallback: ZArchiver not found, so show the general chooser instead.
            Toast.makeText(this, "ZArchiver not found. Showing all options...", Toast.LENGTH_LONG).show();

            Intent generalIntent = new Intent(Intent.ACTION_VIEW);
            generalIntent.setData(folderUri);
            generalIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(generalIntent, "Open Download Folder with");

            if (chooser.resolveActivity(getPackageManager()) != null) {
                startActivity(chooser);
            } else {
                // Final fallback if no file manager is found at all
                showFolderPathDialog(downloadFolder);
            }
        }
    }
    private void showFolderPathDialog(File downloadFolder) {
        String folderPath = downloadFolder.getAbsolutePath();

        new AlertDialog.Builder(this)
                .setTitle("Download Folder")
                .setMessage("Files are saved in:\n\n" + folderPath +
                        "\n\nYou can navigate to this folder using any file manager app like ZArchiver.")
                .setPositiveButton("Copy Path", (dialog, which) -> {
                    // Copy path to clipboard
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Folder Path", folderPath);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Path copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Open Any File Manager", (dialog, which) -> {
                    // Open app drawer filtered for file managers
                    openAppDrawerForFileManagers();
                })
                .setNeutralButton("OK", null)
                .show();
    }

    private void openAppDrawerForFileManagers() {
        try {
            // Open app drawer with file manager related search
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Create chooser with appropriate title
            Intent chooser = Intent.createChooser(intent, "Select File Manager");
            startActivity(chooser);

        } catch (Exception e) {
            Toast.makeText(this, "Please install a file manager app like ZArchiver", Toast.LENGTH_LONG).show();
        }
    }

    // ==================== DATABASE OPERATIONS ====================

    private void saveFileToDatabase(CloudFile file) {
        if (!hasStoragePermission()) {
            Toast.makeText(this, "No storage permission", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            initializeAppFolder();
            File dbFile = getDatabaseFile();

            try (FileOutputStream fos = new FileOutputStream(dbFile, true)) {
                String line = String.format("%s|%s|%d|%d|%s|%b|%s|%s\n",
                        file.getId(),
                        file.getName(),
                        file.getSize(),
                        file.getDate(),
                        file.getPath(),
                        file.isUploaded(),
                        file.getFileId(),
                        file.getMessageId());
                fos.write(line.getBytes());
                fos.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving to database: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFileInDatabase(CloudFile file) {
        if (!hasStoragePermission()) return;

        List<CloudFile> allFiles = new ArrayList<>();
        File dbFile = getDatabaseFile();

        if (dbFile.exists()) {
            try (FileInputStream fis = new FileInputStream(dbFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 6) {
                        String id = parts[0];

                        if (id.equals(file.getId())) {
                            allFiles.add(file);
                        } else {
                            CloudFile existingFile = parseFileLine(parts);
                            if (existingFile != null) {
                                allFiles.add(existingFile);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        rewriteDatabase(allFiles);
    }

    private void deleteFileFromDatabase(CloudFile file) {
        if (!hasStoragePermission()) return;

        List<CloudFile> allFiles = new ArrayList<>();
        File dbFile = getDatabaseFile();

        if (dbFile.exists()) {
            try (FileInputStream fis = new FileInputStream(dbFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 6) {
                        String id = parts[0];

                        if (!id.equals(file.getId())) {
                            CloudFile existingFile = parseFileLine(parts);
                            if (existingFile != null) {
                                allFiles.add(existingFile);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        rewriteDatabase(allFiles);
    }

    private void rewriteDatabase(List<CloudFile> files) {
        if (!hasStoragePermission()) return;

        try {
            initializeAppFolder();
            File dbFile = getDatabaseFile();

            try (FileOutputStream fos = new FileOutputStream(dbFile, false)) {
                for (CloudFile file : files) {
                    String line = String.format("%s|%s|%d|%d|%s|%b|%s|%s\n",
                            file.getId(),
                            file.getName(),
                            file.getSize(),
                            file.getDate(),
                            file.getPath(),
                            file.isUploaded(),
                            file.getFileId(),
                            file.getMessageId());
                    fos.write(line.getBytes());
                }
                fos.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error updating database: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFilesFromDatabase() {
        if (!hasStoragePermission()) {
            Toast.makeText(this, "Storage permission needed to load files", Toast.LENGTH_SHORT).show();
            return;
        }

        cloudFiles.clear();
        File dbFile = getDatabaseFile();

        if (!dbFile.exists()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(dbFile);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            String line;
            int loadedCount = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                CloudFile file = parseFileLine(parts);
                if (file != null) {
                    cloudFiles.add(file);
                    loadedCount++;
                }
            }

            if (loadedCount > 0) {
                Toast.makeText(this, "Loaded " + loadedCount + " files",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading database: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private CloudFile parseFileLine(String[] parts) {
        try {
            if (parts.length >= 8) {
                return new CloudFile(
                        parts[0],
                        parts[1],
                        Long.parseLong(parts[2]),
                        Long.parseLong(parts[3]),
                        parts[4],
                        Boolean.parseBoolean(parts[5]),
                        parts[6],
                        parts[7]
                );
            } else if (parts.length == 6) {
                return new CloudFile(
                        parts[0],
                        parts[1],
                        Long.parseLong(parts[2]),
                        Long.parseLong(parts[3]),
                        parts[4],
                        Boolean.parseBoolean(parts[5])
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ==================== UI & SORTING ====================

    public void updateUI() {
        if (cloudFiles.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerViewFiles.setVisibility(View.GONE);
            tvStorageInfo.setText("0 Files • 0 B Stored");
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerViewFiles.setVisibility(View.VISIBLE);

            long totalSize = 0;
            for (CloudFile file : cloudFiles) {
                totalSize += file.getSize();
            }

            String sizeStr = FileUtils.formatFileSize(totalSize);
            tvStorageInfo.setText(cloudFiles.size() + " Files • " + sizeStr + " Stored");
        }
    }

    private void showSortDialog() {
        String[] options = {"Name (A-Z)", "Name (Z-A)", "Newest First", "Oldest First", "Largest First", "Smallest First"};
        new AlertDialog.Builder(this)
                .setTitle("Sort Files")
                .setItems(options, (dialog, which) -> sortFiles(which))
                .show();
    }

    private void sortFiles(int sortType) {
        switch (sortType) {
            case 0: cloudFiles.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName())); break;
            case 1: cloudFiles.sort((f1, f2) -> f2.getName().compareToIgnoreCase(f1.getName())); break;
            case 2: cloudFiles.sort((f1, f2) -> Long.compare(f2.getDate(), f1.getDate())); break;
            case 3: cloudFiles.sort((f1, f2) -> Long.compare(f1.getDate(), f2.getDate())); break;
            case 4: cloudFiles.sort((f1, f2) -> Long.compare(f2.getSize(), f1.getSize())); break;
            case 5: cloudFiles.sort((f1, f2) -> Long.compare(f1.getSize(), f2.getSize())); break;
        }
        fileAdapter.notifyDataSetChanged();
    }
    // Add this new method to MainActivity.java
    private void shareDownloadedFile(CloudFile file) {
        if (!file.canDownload()) {
            Toast.makeText(this, "File not available for sharing yet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasStoragePermission()) {
            Toast.makeText(this, "Storage permission required to share file", Toast.LENGTH_SHORT).show();
            checkAndRequestPermissions();
            return;
        }

        // Show a progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Preparing to Share")
                .setMessage("Downloading " + file.getName() + "...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            TelegramDownloader downloader = new TelegramDownloader(
                    getApplicationContext(),
                    BOT_TOKEN,
                    file.getFileId(),
                    file.getName(),
                    new TelegramDownloader.DownloadCallback() {
                        @Override
                        public void onDownloadProgress(int progress) {
                            // Not needed for this feature, can be left empty
                        }

                        @Override
                        public void onDownloadSuccess(Uri fileUri) { // This is a file:// URI
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                try {
                                    File fileToShare = new File(fileUri.getPath());

                                    // Get a secure content URI using FileProvider
                                    Uri contentUri = FileProvider.getUriForFile(MainActivity.this,
                                            getPackageName() + ".fileprovider",
                                            fileToShare);

                                    // Create the share intent
                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType(FileUtils.getMimeType(file.getName()));
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                    // Start the chooser and schedule the temp file for deletion
                                    startActivity(Intent.createChooser(shareIntent, "Share " + file.getName()));
                                    scheduleTempFileDeletion(fileToShare);

                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "Error sharing file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        @Override
                        public void onDownloadFailed(String error) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Share Failed")
                                        .setMessage("Could not download file for sharing.\n\nError: " + error)
                                        .setPositiveButton("OK", null)
                                        .show();
                            });
                        }
                    }
            );
            downloader.download();
        }).start();
    }

    // Add this helper method for cleaning up the temp file
    private void scheduleTempFileDeletion(File file) {
        // Deletes the file after 15 seconds
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (file != null && file.exists()) {
                if (file.delete()) {
                    android.util.Log.i("FileCleanup", "Temporary share file deleted: " + file.getName());
                } else {
                    android.util.Log.w("FileCleanup", "Failed to delete temporary share file: " + file.getName());
                }
            }
        }, 15000); // 15-second delay
    }
    // Add this new method to MainActivity.java
    private void deleteFileFromVault(CloudFile file, int position) {
        // Check if the file has a messageId. If not, it can't be deleted from Telegram.
        if (!file.isUploaded() || file.getMessageId() == null || file.getMessageId().isEmpty()) {
            // If the file was never uploaded, just remove it locally.
            cloudFiles.remove(position);
            fileAdapter.notifyItemRemoved(position);
            deleteFileFromDatabase(file);
            updateUI();
            Snackbar.make(findViewById(android.R.id.content),
                    "Removed " + file.getName() + " from list.",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Show a progress dialog for cloud deletion
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Deleting from Vault")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            TelegramDeleter deleter = new TelegramDeleter(
                    BOT_TOKEN,
                    CHAT_ID,
                    file.getMessageId(),
                    new TelegramDeleter.DeleteCallback() {
                        @Override
                        public void onDeleteSuccess() {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                // Remove from UI list
                                cloudFiles.remove(position);
                                fileAdapter.notifyItemRemoved(position);
                                // Remove from local database
                                deleteFileFromDatabase(file);
                                // Update storage info
                                updateUI();
                                Snackbar.make(findViewById(android.R.id.content),
                                        "✓ Deleted " + file.getName() + " from the cloud.",
                                        Snackbar.LENGTH_LONG).show();
                            });
                        }

                        @Override
                        public void onDeleteFailed(String error) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Deletion Failed")
                                        .setMessage("Could not delete '" + file.getName() + "' from Telegram.\n\nError: " + error)
                                        .setPositiveButton("OK", null)
                                        .show();
                            });
                        }
                    }
            );
            deleter.delete();
        }).start();
    }
}
