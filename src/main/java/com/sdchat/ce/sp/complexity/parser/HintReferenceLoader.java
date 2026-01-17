package com.sdchat.ce.sp.complexity.parser;

import com.sdchat.ce.sp.complexity.model.HintReference;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HintReferenceLoader {

    private static final String HINT_REFERENCE_FILE = "gaussdb_sql_plan_hints.json";

    private final Map<String, HintReference> hintRegistry = new HashMap<>();
    private final List<HintReference> allHints = new ArrayList<>();
    private boolean loaded = false;

    @PostConstruct
    public void init() {
        loadHintReferences();
    }

    public void loadHintReferences() {
        try {
            ClassPathResource resource = new ClassPathResource(HINT_REFERENCE_FILE);
            if (!resource.exists()) {
                log.warn("Hint reference file {} not found in classpath", HINT_REFERENCE_FILE);
                return;
            }

            StringBuilder jsonContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line);
                }
            }

            parseAndRegisterHints(jsonContent.toString());
            loaded = true;
            log.info("Successfully loaded {} hint references from {}", allHints.size(), HINT_REFERENCE_FILE);

        } catch (Exception e) {
            log.error("Failed to load hint reference file: {}", e.getMessage(), e);
        }
    }

    private void parseAndRegisterHints(String jsonContent) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(jsonContent);
            com.fasterxml.jackson.databind.JsonNode hintsNode = root.get("hints");

            if (hintsNode == null || !hintsNode.isArray()) {
                log.warn("No hints array found in reference file");
                return;
            }

            for (com.fasterxml.jackson.databind.JsonNode hintNode : hintsNode) {
                HintReference hint = parseHintNode(hintNode);
                if (hint != null) {
                    allHints.add(hint);
                    hintRegistry.put(hint.getHint().toLowerCase(), hint);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse hint reference JSON: {}", e.getMessage(), e);
        }
    }

    private HintReference parseHintNode(com.fasterxml.jackson.databind.JsonNode node) {
        try {
            String hintName = node.path("hint").asText();
            if (hintName == null || hintName.isEmpty()) {
                return null;
            }

            HintReference.HintReferenceBuilder builder = HintReference.builder()
                    .hint(hintName)
                    .category(node.path("category").asText(""))
                    .description(node.path("description").asText(""))
                    .syntax(node.path("syntax").asText(""))
                    .example(node.path("example").asText(""))
                    .notes(node.path("notes").asText(""));

            com.fasterxml.jackson.databind.JsonNode paramsNode = node.get("parameters");
            if (paramsNode != null && paramsNode.isArray()) {
                List<com.sdchat.ce.sp.complexity.model.HintParameter> params = new ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode paramNode : paramsNode) {
                    com.sdchat.ce.sp.complexity.model.HintParameter param = com.sdchat.ce.sp.complexity.model.HintParameter.builder()
                            .name(paramNode.path("name").asText(""))
                            .description(paramNode.path("description").asText(""))
                            .required(paramNode.path("no").isMissingNode())
                            .build();
                    params.add(param);
                }
                builder.parameters(params);
            }

            return builder.build();

        } catch (Exception e) {
            log.warn("Failed to parse hint node: {}", e.getMessage());
            return null;
        }
    }

    public HintReference getHintReference(String hintName) {
        if (!loaded) {
            loadHintReferences();
        }
        return hintRegistry.get(hintName.toLowerCase());
    }

    public List<HintReference> getAllHints() {
        if (!loaded) {
            loadHintReferences();
        }
        return new ArrayList<>(allHints);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public int getHintCount() {
        if (!loaded) {
            loadHintReferences();
        }
        return allHints.size();
    }
}
