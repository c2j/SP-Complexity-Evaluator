package com.sdchat.ce.sp.complexity.service;

import com.sdchat.ce.sp.complexity.model.WeightConfiguration;
import com.sdchat.ce.sp.complexity.model.WeightTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing weight configurations and templates.
 * Provides methods to create, save, load, and delete weight templates.
 */
@Service
@Slf4j
public class WeightService {

    private final Map<String, WeightTemplate> templates = new HashMap<>();

    /**
     * Get all weight templates.
     */
    public Map<String, WeightTemplate> getAllTemplates() {
        return new HashMap<>(templates);
    }

    /**
     * Save a weight configuration as a template.
     */
    public WeightTemplate saveAsTemplate(String name, String dialect, Boolean isShared, WeightConfiguration weights, String createdBy) {
        WeightTemplate template = new WeightTemplate(name, dialect, isShared, weights, createdBy);
        templates.put(template.getId(), template);

        log.info("Saved weight template: {} for dialect: {}", name, dialect);
        return template;
    }

    /**
     * Load a weight template.
     */
    public WeightTemplate loadTemplate(String id) {
        WeightTemplate template = templates.get(id);
        if (template == null) {
            log.warn("Weight template not found with id: {}", id);
            return null;
        }

        log.info("Loaded weight template: {} for dialect: {}", template.getName(), template.getDialect());
        return template;
    }

    /**
     * Delete a weight template.
     */
    public boolean deleteTemplate(String id) {
        WeightTemplate template = templates.remove(id);
        if (template != null) {
            log.info("Deleted weight template: {}", template.getName());
            return true;
        }

        log.warn("Weight template not found with id: {}", id);
        return false;
    }
}