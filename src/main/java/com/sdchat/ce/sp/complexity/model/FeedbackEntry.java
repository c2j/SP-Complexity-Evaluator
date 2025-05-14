package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Model class representing a feedback entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackEntry {

    /**
     * The type of feedback (SQL, Procedure, ZIP).
     */
    private String feedbackType;

    /**
     * Brief description of the content that the feedback is about.
     */
    private String contentDescription;

    /**
     * The dialect of the SQL or procedure.
     */
    private String dialect;

    /**
     * User comments about the inaccuracy.
     */
    private String comments;

    /**
     * The path to the uploaded file containing the content.
     */
    private String filePath;

    /**
     * The original filename of the uploaded file.
     */
    private String originalFilename;

    /**
     * Contact person name (optional).
     */
    private String contactPerson;

    /**
     * Contact information such as email or phone (optional).
     */
    private String contactInfo;

    /**
     * The timestamp when the feedback was submitted.
     */
    private LocalDateTime timestamp;
}
