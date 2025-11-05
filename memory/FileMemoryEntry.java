package memory;

import java.time.LocalDateTime;

/**
 * Represents a file memory entry in the memory store.
 */
public class FileMemoryEntry {

    private final String id;
    private final String fileName;
    private final String filePath;
    private final String content;
    private final long size;
    private final LocalDateTime created;

    public FileMemoryEntry(String id, String fileName, String filePath, String content) {
        this.id = id;
        this.fileName = fileName;
        this.filePath = filePath;
        this.content = content;
        this.size = content != null ? content.length() : 0;
        this.created = LocalDateTime.now();
    }

    // Getters
    public String getId() { return id; }
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public String getContent() { return content; }
    public long getSize() { return size; }
    public LocalDateTime getCreated() { return created; }
}