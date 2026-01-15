package com.sdchat.ce.sp.complexity.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for built-in function filtering feature.
 * Handles loading and initialization of the gaussdb_functions.json file.
 */
@Slf4j
@Configuration
public class BuiltInFunctionConfig {

    public BuiltInFunctionConfig() {
        log.debug("BuiltInFunctionConfig initialized - gaussdb_functions.json will be loaded by BuiltInFunctionFilter");
    }
}
