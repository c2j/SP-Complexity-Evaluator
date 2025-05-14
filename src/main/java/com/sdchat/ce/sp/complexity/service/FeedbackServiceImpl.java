package com.sdchat.ce.sp.complexity.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sdchat.ce.sp.complexity.model.FeedbackEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of the feedback service.
 */
@Slf4j
@Service
public class FeedbackServiceImpl implements FeedbackService {

    private static final String FEEDBACK_FILE = "feedback.json";
    private static final String FEEDBACK_FILES_DIR = "feedback-files";
    private final ObjectMapper objectMapper;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Path feedbackFilePath;
    private final Path feedbackFilesDir;

    public FeedbackServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // Create feedback file in the user's home directory or current working directory
        String userHome = System.getProperty("user.home");
        Path feedbackDir;

        if (userHome != null && !userHome.isEmpty()) {
            feedbackDir = Paths.get(userHome, ".sp-complexity-evaluator");
        } else {
            feedbackDir = Paths.get(".");
        }

        this.feedbackFilePath = feedbackDir.resolve(FEEDBACK_FILE);
        this.feedbackFilesDir = feedbackDir.resolve(FEEDBACK_FILES_DIR);
    }

    @PostConstruct
    public void init() {
        try {
            // Create the parent directory if it doesn't exist
            Path parent = feedbackFilePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
                log.info("Created feedback directory: {}", parent);
            }

            // Create the feedback file if it doesn't exist
            if (!Files.exists(feedbackFilePath)) {
                Files.createFile(feedbackFilePath);
                // Initialize with an empty array
                Files.writeString(feedbackFilePath, "[]");
                log.info("Created new feedback file: {}", feedbackFilePath);
            } else {
                // Check if the feedback file is valid JSON
                if (!isValidJsonFile(feedbackFilePath)) {
                    // Backup the corrupted file and create a new one
                    backupAndResetFeedbackFile();
                } else {
                    // Migrate existing feedback entries if needed
                    migrateExistingFeedbackEntries();
                }
            }

            // Create the feedback files directory if it doesn't exist
            if (!Files.exists(feedbackFilesDir)) {
                Files.createDirectories(feedbackFilesDir);
                log.info("Created feedback files directory: {}", feedbackFilesDir);
            }
        } catch (IOException e) {
            log.error("Failed to create feedback file or directory", e);
        }
    }

    /**
     * Checks if a file contains valid JSON.
     *
     * @param path The path to the file
     * @return true if the file contains valid JSON, false otherwise
     */
    private boolean isValidJsonFile(Path path) {
        try {
            if (Files.size(path) == 0) {
                return false;
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(path.toFile());
            return true;
        } catch (Exception e) {
            log.error("Invalid JSON file: {}", path, e);
            return false;
        }
    }

    /**
     * Backs up the corrupted feedback file and creates a new empty one.
     */
    private void backupAndResetFeedbackFile() {
        try {
            // Create a backup file with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backupPath = feedbackFilePath.resolveSibling(feedbackFilePath.getFileName() + "." + timestamp + ".bak");

            // Copy the corrupted file to the backup
            Files.copy(feedbackFilePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Backed up corrupted feedback file to: {}", backupPath);

            // Create a new empty feedback file
            Files.writeString(feedbackFilePath, "[]");
            log.info("Reset feedback file to empty array");
        } catch (IOException e) {
            log.error("Failed to backup and reset feedback file", e);
        }
    }

    /**
     * Migrates existing feedback entries from the old format to the new format.
     * This handles the field name change from "content" to "contentDescription".
     */
    private void migrateExistingFeedbackEntries() {
        try {
            if (!Files.exists(feedbackFilePath) || Files.size(feedbackFilePath) == 0) {
                return;
            }

            // Read the file as a generic JSON structure
            ObjectMapper reader = new ObjectMapper();
            reader.registerModule(new JavaTimeModule());

            // Read as a list of maps to handle the field name change
            List<Map<String, Object>> entries = reader.readValue(
                    feedbackFilePath.toFile(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            boolean needsMigration = false;

            // Check if any entry has the old "content" field
            for (Map<String, Object> entry : entries) {
                if (entry.containsKey("content") && !entry.containsKey("contentDescription")) {
                    // Migrate the field
                    entry.put("contentDescription", entry.get("content"));
                    entry.remove("content");
                    needsMigration = true;
                }
            }

            // If migration is needed, write the updated entries back to the file
            if (needsMigration) {
                objectMapper.writeValue(feedbackFilePath.toFile(), entries);
                log.info("Migrated feedback entries from 'content' to 'contentDescription'");
            }
        } catch (IOException e) {
            log.error("Failed to migrate feedback entries", e);
        }
    }

    @Override
    public FeedbackEntry saveFeedbackWithFile(
            String feedbackType,
            String contentDescription,
            String dialect,
            String comments,
            MultipartFile file,
            String contactPerson,
            String contactInfo) throws IOException {

        // Generate a unique filename to avoid collisions
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String newFilename = timestamp + "_" + uniqueId + extension;
        Path filePath = feedbackFilesDir.resolve(newFilename);

        // Save the uploaded file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Saved feedback file: {}", filePath);

        // Create and save the feedback entry
        FeedbackEntry feedback = FeedbackEntry.builder()
                .feedbackType(feedbackType)
                .contentDescription(contentDescription)
                .dialect(dialect)
                .comments(comments)
                .filePath(filePath.toString())
                .originalFilename(originalFilename)
                .contactPerson(contactPerson)
                .contactInfo(contactInfo)
                .timestamp(LocalDateTime.now())
                .build();

        lock.writeLock().lock();
        try {
            // Read existing feedback
            List<FeedbackEntry> feedbackEntries = readFeedbackFile();

            // Add new feedback
            feedbackEntries.add(feedback);

            // Write back to file
            objectMapper.writeValue(feedbackFilePath.toFile(), feedbackEntries);

            log.info("Saved new feedback entry: {}", feedback);
            return feedback;
        } catch (IOException e) {
            // If we fail to save the feedback entry, delete the uploaded file
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException deleteEx) {
                log.error("Failed to delete feedback file after error", deleteEx);
            }

            log.error("Failed to save feedback", e);
            throw e;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<FeedbackEntry> getAllFeedback() {
        lock.readLock().lock();
        try {
            return readFeedbackFile();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String getFeedbackFileContent(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        // Security check: ensure the file is within our feedback files directory
        if (!path.normalize().startsWith(feedbackFilesDir.normalize())) {
            throw new SecurityException("Attempted to access file outside of feedback directory: " + filePath);
        }

        if (!Files.exists(path)) {
            throw new IOException("Feedback file not found: " + filePath);
        }

        return Files.readString(path);
    }

    @Override
    public byte[] getFeedbackFileBytes(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        // Security check: ensure the file is within our feedback files directory
        if (!path.normalize().startsWith(feedbackFilesDir.normalize())) {
            throw new SecurityException("Attempted to access file outside of feedback directory: " + filePath);
        }

        if (!Files.exists(path)) {
            throw new IOException("Feedback file not found: " + filePath);
        }

        return Files.readAllBytes(path);
    }

    private List<FeedbackEntry> readFeedbackFile() {
        try {
            if (!Files.exists(feedbackFilePath) || Files.size(feedbackFilePath) == 0) {
                return new ArrayList<>();
            }

            // Configure ObjectMapper to ignore unknown properties
            ObjectMapper reader = new ObjectMapper();
            reader.registerModule(new JavaTimeModule());
            reader.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // Read the file and convert to FeedbackEntry list
            List<FeedbackEntry> entries = reader.readValue(
                    feedbackFilePath.toFile(),
                    new TypeReference<List<FeedbackEntry>>() {}
            );

            // Handle potential null list
            return entries != null ? new ArrayList<>(entries) : new ArrayList<>();
        } catch (IOException e) {
            log.error("Failed to read feedback file", e);
            // Return a mutable empty list instead of Collections.emptyList()
            return new ArrayList<>();
        }
    }
}
