package com.tnl.entity;

public class FileRecord {

    private String fileName;
    private String importDate;
    private String folderName; // Add this field

    public FileRecord(String fileName, String importDate, String folderName) {
        this.fileName = fileName;
        this.importDate = importDate;
        this.folderName = folderName; // Initialize the folder name
    }

    public String getFileName() {
        return fileName;
    }

    public String getImportDate() {
        return importDate;
    }

    public String getFolderName() {
        return folderName; // Getter for folder name
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setImportDate(String importDate) {
        this.importDate = importDate;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
}
