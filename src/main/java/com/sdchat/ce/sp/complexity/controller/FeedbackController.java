package com.sdchat.ce.sp.complexity.controller;

import com.sdchat.ce.sp.complexity.model.FeedbackEntry;
import com.sdchat.ce.sp.complexity.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Controller for handling user feedback.
 */
@Slf4j
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Submit feedback with a file about inaccuracies in SQL, procedure, or ZIP evaluations.
     *
     * @param feedbackType The type of feedback (SQL, Procedure, ZIP)
     * @param contentDescription Brief description of the content
     * @param dialect The SQL dialect
     * @param comments User comments about the inaccuracy
     * @param file The uploaded file containing the content
     * @return The saved feedback entry
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FeedbackEntry> submitFeedback(
            @RequestParam("feedbackType") String feedbackType,
            @RequestParam("contentDescription") String contentDescription,
            @RequestParam("dialect") String dialect,
            @RequestParam("comments") String comments,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "contactPerson", required = false) String contactPerson,
            @RequestParam(value = "contactInfo", required = false) String contactInfo) {

        log.info("Received feedback: type={}, description={}, dialect={}, file={}, contactPerson={}",
                feedbackType, contentDescription, dialect, file.getOriginalFilename(), contactPerson);

        try {
            FeedbackEntry savedFeedback = feedbackService.saveFeedbackWithFile(
                    feedbackType, contentDescription, dialect, comments, file, contactPerson, contactInfo);
            return ResponseEntity.ok(savedFeedback);
        } catch (IOException e) {
            log.error("Failed to save feedback with file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all feedback entries.
     *
     * @return A list of all feedback entries
     */
    @GetMapping
    public ResponseEntity<List<FeedbackEntry>> getAllFeedback() {
        List<FeedbackEntry> feedbackEntries = feedbackService.getAllFeedback();
        return ResponseEntity.ok(feedbackEntries);
    }

    /**
     * Get the content of a feedback file.
     *
     * @param filePath The path to the feedback file
     * @return The content of the file as a string
     */
    @GetMapping("/file")
    public ResponseEntity<String> getFeedbackFileContent(@RequestParam("path") String filePath) {
        try {
            String content = feedbackService.getFeedbackFileContent(filePath);
            return ResponseEntity.ok(content);
        } catch (SecurityException e) {
            log.error("Security violation: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid file path");
        } catch (IOException e) {
            log.error("Failed to read feedback file", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Download a feedback file.
     *
     * @param filePath The path to the feedback file
     * @param originalFilename The original filename to use in the Content-Disposition header
     * @return The file as a resource
     */
    @GetMapping("/file/download")
    public ResponseEntity<byte[]> downloadFeedbackFile(
            @RequestParam("path") String filePath,
            @RequestParam(value = "filename", required = false) String originalFilename) {
        try {
            byte[] fileContent = feedbackService.getFeedbackFileBytes(filePath);

            // Determine filename for the download
            String filename = originalFilename;
            if (filename == null || filename.isEmpty()) {
                Path path = Paths.get(filePath);
                filename = path.getFileName().toString();
            }

            // Set appropriate headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

            // Try to determine content type
            String contentType = determineContentType(filename);
            headers.setContentType(MediaType.parseMediaType(contentType));

            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (SecurityException e) {
            log.error("Security violation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Failed to read feedback file for download", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Determine the content type based on the file extension.
     *
     * @param filename The filename
     * @return The content type
     */
    private String determineContentType(String filename) {
        if (filename == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        String lowercaseFilename = filename.toLowerCase();
        if (lowercaseFilename.endsWith(".sql")) {
            return "text/plain";
        } else if (lowercaseFilename.endsWith(".txt")) {
            return "text/plain";
        } else if (lowercaseFilename.endsWith(".json")) {
            return "application/json";
        } else if (lowercaseFilename.endsWith(".xml")) {
            return "application/xml";
        } else if (lowercaseFilename.endsWith(".zip")) {
            return "application/zip";
        } else {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}
