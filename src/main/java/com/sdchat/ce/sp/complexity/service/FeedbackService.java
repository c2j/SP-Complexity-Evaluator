package com.sdchat.ce.sp.complexity.service;

import com.sdchat.ce.sp.complexity.model.FeedbackEntry;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Service interface for handling user feedback.
 */
public interface FeedbackService {

    /**
     * Save a feedback entry with an uploaded file.
     *
     * @param feedbackType The type of feedback (SQL, Procedure, ZIP)
     * @param contentDescription Brief description of the content
     * @param dialect The SQL dialect
     * @param comments User comments about the inaccuracy
     * @param file The uploaded file containing the content
     * @param contactPerson Contact person name (optional)
     * @param contactInfo Contact information such as email or phone (optional)
     * @return The saved feedback entry
     * @throws IOException If there is an error handling the file
     */
    FeedbackEntry saveFeedbackWithFile(
            String feedbackType,
            String contentDescription,
            String dialect,
            String comments,
            MultipartFile file,
            String contactPerson,
            String contactInfo) throws IOException;

    /**
     * Get all feedback entries.
     *
     * @return A list of all feedback entries
     */
    List<FeedbackEntry> getAllFeedback();

    /**
     * Get the content of a feedback file as a string.
     *
     * @param filePath The path to the feedback file
     * @return The content of the file as a string
     * @throws IOException If there is an error reading the file
     */
    String getFeedbackFileContent(String filePath) throws IOException;

    /**
     * Get the content of a feedback file as a byte array.
     *
     * @param filePath The path to the feedback file
     * @return The content of the file as a byte array
     * @throws IOException If there is an error reading the file
     */
    byte[] getFeedbackFileBytes(String filePath) throws IOException;
}
