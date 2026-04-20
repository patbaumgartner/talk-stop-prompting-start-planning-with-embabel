package com.patbaumgartner.embabel.guardrails;

import com.patbaumgartner.embabel.guardrails.GuardrailModels.AnalyzeRequest;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.AnalyzeResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * Declarative HTTP client for Microsoft Presidio Analyzer. Presidio runs as a sidecar
 * container (see docker-compose.yml).
 */
@HttpExchange
public interface PresidioAnalyzerClient {

	@PostExchange("/analyze")
	List<AnalyzeResult> analyze(@RequestBody AnalyzeRequest request);

}
