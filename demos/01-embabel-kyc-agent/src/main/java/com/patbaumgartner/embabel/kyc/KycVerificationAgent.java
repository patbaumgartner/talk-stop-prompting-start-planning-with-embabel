package com.patbaumgartner.embabel.kyc;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.patbaumgartner.embabel.kyc.KycModels.KycAssessment;
import com.patbaumgartner.embabel.kyc.KycModels.KycRequest;
import com.patbaumgartner.embabel.kyc.KycModels.KycScreening;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KYC Verification Agent — 2 actions, fully planned by Embabel.
 *
 * Plan: KycRequest → screenCustomer → KycScreening → assessRisk → KycAssessment
 *
 * Key demo points: - .withoutProperties("customerId") shows property filtering (exclude
 * internal IDs from LLM context) - Domain objects ARE the LLM tools — the planner
 * connects them automatically - Both actions use the LLM — this is a "think → decide"
 * pattern
 */
@Agent(name = "KycVerificationAgent",
		description = "Screens a customer against risk indicators and produces a risk assessment.", version = "1.0.0")
public class KycVerificationAgent {

	private static final Logger log = LoggerFactory.getLogger(KycVerificationAgent.class);

	/**
	 * Step 1: Screen the customer against PEP lists, sanctions, and adverse media.
	 *
	 * Uses .withoutProperties("customerId") to exclude the internal ID from the LLM
	 * prompt. The LLM sees business data, but not the database key.
	 */
	@Action(description = "Screen customer data against PEP lists, sanctions databases, and adverse media.")
	public KycScreening screenCustomer(KycRequest request, OperationContext context) {
		log.info("Screening customer: {}", request.fullName());

		var prompt = """
				You are a KYC compliance screening system.
				Analyze the following customer data and check against known risk indicators.

				Customer:
				- Full Name: %s
				- Date of Birth: %s
				- Nationality: %s
				- Occupation: %s
				- Source of Funds: %s

				Determine:
				1. Whether the name matches any known Politically Exposed Persons (PEP)
				2. Whether the name or nationality triggers sanctions screening alerts
				3. Whether there is adverse media associated with this person

				Provide a screening summary explaining your findings.
				""".formatted(request.fullName(), request.dateOfBirth(), request.nationality(),
				request.occupation() != null ? request.occupation() : "not provided",
				request.sourceOfFunds() != null ? request.sourceOfFunds() : "not provided");

		return context.ai()
			.withDefaultLlm()
			.creating(KycScreening.class)
			.withoutProperties("customerId")
			.fromPrompt(prompt);
	}

	/**
	 * Step 2: Assess overall risk based on screening results.
	 */
	@AchievesGoal(description = "Produce a risk assessment for a KYC verification request.")
	@Action(description = "Assess overall KYC risk based on screening results and produce a recommendation.")
	public KycAssessment assessRisk(KycScreening screening, OperationContext context) {
		log.info("Assessing risk for: {} (PEP={}, Sanctions={}, AdverseMedia={})", screening.fullName(),
				screening.pepMatch(), screening.sanctionsMatch(), screening.adverseMediaMatch());

		var prompt = """
				You are a KYC risk assessment engine.
				Based on the screening results below, determine the overall risk level and recommendation.

				Screening Results:
				- Full Name: %s
				- Nationality: %s
				- PEP Match: %s
				- Sanctions Match: %s
				- Adverse Media Match: %s
				- Screening Summary: %s

				Risk levels: LOW, MEDIUM, HIGH, CRITICAL
				Recommendations: APPROVE, ENHANCED_DUE_DILIGENCE, REJECT, ESCALATE_TO_COMPLIANCE

				Rules:
				- Sanctions match → CRITICAL risk, REJECT
				- PEP match → HIGH risk, ENHANCED_DUE_DILIGENCE
				- Adverse media only → MEDIUM risk, ENHANCED_DUE_DILIGENCE
				- No flags → LOW risk, APPROVE

				Provide a clear justification for your decision.
				""".formatted(screening.fullName(), screening.nationality(), screening.pepMatch(),
				screening.sanctionsMatch(), screening.adverseMediaMatch(), screening.screeningSummary());

		return context.ai().withDefaultLlm().creating(KycAssessment.class).fromPrompt(prompt);
	}

}
