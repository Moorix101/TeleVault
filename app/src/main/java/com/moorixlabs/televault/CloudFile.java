package com.moorixlabs.televault;

public class CloudFile {
    private String id;
    private String name;
    private long size;
    private long date;
    private String path;
    private boolean uploaded;
    private int uploadProgress;
    private String fileId;      // Telegram file_id
    private String messageId;   // Telegram message_id

    // Constructor for new files (without Telegram IDs)
    public CloudFile(String id, String name, long size, long date, String path, boolean uploaded) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.date = date;
        this.path = path;
        this.uploaded = uploaded;
        this.uploadProgress = 0;
        this.fileId = "";
        this.messageId = "";
    }

    // Constructor for uploaded files (with Telegram IDs)
    public CloudFile(String id, String name, long size, long date, String path,
                     boolean uploaded, String fileId, String messageId) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.date = date;
        this.path = path;
        this.uploaded = uploaded;
        this.uploadProgress = uploaded ? 100 : 0;
        this.fileId = fileId != null ? fileId : "";
        this.messageId = messageId != null ? messageId : "";
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public long getDate() {
        return date;
    }

    public String getPath() {
        return path;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public int getUploadProgress() {
        return uploadProgress;
    }

    public String getFileId() {
        return fileId;
    }

    public String getMessageId() {
        return messageId;
    }

    // Setters
    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public void setUploadProgress(int uploadProgress) {
        this.uploadProgress = uploadProgress;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId != null ? fileId : "";
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId != null ? messageId : "";
    }

    // Utility methods
    public String getFileExtension() {
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Check if this file can be downloaded from Telegram
     * @return true if file has been uploaded and has a valid fileId
     */
    public boolean canDownload() {
        return uploaded && fileId != null && !fileId.isEmpty();
    }

    @Override
    public String toString() {
        return "CloudFile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", uploaded=" + uploaded +
                ", fileId='" + fileId + '\'' +
                ", messageId='" + messageId + '\'' +
                '}';
    }
}