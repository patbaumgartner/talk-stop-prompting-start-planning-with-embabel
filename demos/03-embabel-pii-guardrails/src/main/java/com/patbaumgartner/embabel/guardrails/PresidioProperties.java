package com.patbaumgartner.embabel.guardrails;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Presidio Analyzer configuration. Maps to presidio.analyzer.* in application.properties.
 */
@ConfigurationProperties(prefix = "presidio.analyzer")
public record PresidioProperties(String baseUrl, List<String> piiTypes) {
}
