package pl.polsl.survival.actionrules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.tools.container.Pair;

import adaa.analytics.rules.logic.induction.ActionCovering;
import adaa.analytics.rules.logic.induction.Covering;
import adaa.analytics.rules.logic.quality.LogRank;
import adaa.analytics.rules.logic.representation.Action;
import adaa.analytics.rules.logic.representation.ActionRule;
import adaa.analytics.rules.logic.representation.CompoundCondition;
import adaa.analytics.rules.logic.representation.ConditionBase;
import adaa.analytics.rules.logic.representation.ElementaryCondition;
import adaa.analytics.rules.logic.representation.KaplanMeierEstimator;
import adaa.analytics.rules.logic.representation.Rule;
import adaa.analytics.rules.logic.representation.SingletonSet;
import adaa.analytics.rules.logic.representation.SurvivalRule;

public class SurvivalActionRule extends ActionRule {


	/**
	 * 
	 */
	private static final long serialVersionUID = 948434570473531014L;

	public static Action createConsequence(String survivalStatusName) {
		Action consequence = new Action(
				survivalStatusName,
				new SingletonSet(Double.NaN, null),
				new SingletonSet(Double.NaN, null));
		return consequence;
	}

	public SurvivalActionRule(String survivalStatusName) {
		if (survivalStatusName == null) {
			survivalStatusName = "survival_status";
		}
		Action consequence = createConsequence(survivalStatusName);
		this.premise = new CompoundCondition();
		this.actionConsequence = consequence;
	}

	public SurvivalActionRule(CompoundCondition premise, Action consequence) {
		super(premise, consequence);
	}

	@Override
	public SurvivalRule getLeftRule() {
		CompoundCondition premise = new CompoundCondition();
		for (ConditionBase a : this.getPremise().getSubconditions()) {
			if (a.isDisabled()) {
				continue;
			}
			Action ac = (Action)a;
			premise.addSubcondition(new ElementaryCondition(ac.getAttribute(), ac.getLeftValue()));
		}

		SurvivalRule rule = new SurvivalRule(premise, new ElementaryCondition(actionConsequence.getAttribute(), actionConsequence.getLeftValue()));

		return rule;
	}

	@Override
	public SurvivalRule getRightRule() {

		CompoundCondition premise = new CompoundCondition();
		for (ConditionBase a : this.getPremise().getSubconditions()) {
			if (a.isDisabled()){
				continue;
			}
			Action ac = (Action)a;
			if (ac.getRightValue() != null && !ac.getActionNil()) {
				premise.addSubcondition(new ElementaryCondition(ac.getAttribute(), ac.getRightValue()));
			}
		}

		SurvivalRule rule = new SurvivalRule(premise, new ElementaryCondition(actionConsequence.getAttribute(), actionConsequence.getLeftValue()));

		return rule;
	}

	@Override
	public Covering covers(ExampleSet set) {

		ActionCovering covered = new ActionCovering();

		Rule rightRule = this.getRightRule();
		Rule leftRule = this.getLeftRule();

		Covering leftCov = leftRule.covers(set);
		Covering rightCov = rightRule.covers(set);

		covered.weighted_p = leftCov.weighted_p;
		covered.weighted_pRight =  rightCov.weighted_p;
		covered.weighted_n = leftCov.weighted_n;
		covered.weighted_nRight =  rightCov.weighted_n;
		covered.weighted_P = leftCov.weighted_P;
		covered.weighted_N = leftCov.weighted_N;
		covered.weighted_P_right = rightCov.weighted_P;
		covered.weighted_N_right = rightCov.weighted_N;

		covered.positives = leftCov.positives;
		covered.negatives = leftCov.negatives;

		return covered;
	}

	public Pair<Double, Double> getLogRankResults(ExampleSet exampleSet) {
		Covering leftRuleCovering = this.getLeftRule().covers(exampleSet);
		Covering rightRuleCovering = this.getRightRule().covers(exampleSet);
		KaplanMeierEstimator leftRuleKM = new KaplanMeierEstimator(exampleSet, leftRuleCovering.positives);
		KaplanMeierEstimator rightRuleKM = new KaplanMeierEstimator(exampleSet, rightRuleCovering.positives);
		LogRank logrank = new LogRank();
		Pair<Double, Double> logrankResult = logrank.calculate(leftRuleKM, rightRuleKM);
		return logrankResult;
	}

	public double getLogRankValueLeftRightRule(ExampleSet exampleSet) {
		Pair<Double, Double> logrankResult = getLogRankResults(exampleSet);
		return logrankResult.getFirst();
	}

	public double getPValueLogRank(ExampleSet exampleSet) {
		Pair<Double, Double> logrankResult = getLogRankResults(exampleSet);
		return logrankResult.getSecond();
	}

	public static float getCommonExamplesPercentage(SurvivalRule leftRule, SurvivalRule rightRule, ExampleSet exampleSet)
	{
		Covering leftRuleCovering = leftRule.covers(exampleSet);
		Set<Integer> intersection = new HashSet<Integer>(leftRuleCovering.positives);
		Covering rightRuleCovering = rightRule.covers(exampleSet);
		intersection.retainAll(rightRuleCovering.positives);
		float commonExamplesPercentage = (float) intersection.size()/exampleSet.size();

		return commonExamplesPercentage;
	}

	public float getCommonExamplesPercentage(ExampleSet exampleSet)
	{
		float commonExamplesPercentage = SurvivalActionRule.getCommonExamplesPercentage(this.getLeftRule(), this.getRightRule(), exampleSet);

		return commonExamplesPercentage;
	}

	public float getRuleCoveringPercentage(ExampleSet exampleSet)
	{
		float ruleCoveringPercentage = (float) this.covers(exampleSet).positives.size()/exampleSet.size();
		
		return ruleCoveringPercentage;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SurvivalActionRule)) {
			return false;
		}
		SurvivalActionRule survActionRule = (SurvivalActionRule)obj;
		return (this.getLeftRule().getPremise().equals(survActionRule.getLeftRule().getPremise()) &
				this.getRightRule().getPremise().equals(survActionRule.getRightRule().getPremise()) &
				this.getLeftRule().getConsequence().equals(survActionRule.getLeftRule().getConsequence()) &
				this.getRightRule().getConsequence().equals(survActionRule.getRightRule().getConsequence()));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.consequence, this.premise);
	}

	@Override
	public String toString() {
		String premiseText = premise == null ? "" :premise.toString();
		String consequenceText = "(" + actionConsequence.getLeftCondition().toString() + ")";
		String s = "IF " + premiseText + " THEN " + consequenceText;
		return s;
	}
	
	public SurvivalActionRule reduceActionsThisSameAttribute() {
		SurvivalActionRule newRule = new SurvivalActionRule(this.actionConsequence.getAttribute());
		Map<String, ElementaryCondition> shortened = new HashMap<String, ElementaryCondition>();
		Set<ConditionBase> unshortened = new HashSet<ConditionBase>();
		List<ConditionBase> subconditions = this.getPremise().getSubconditions();
		for (ConditionBase cnd : subconditions) {
			if (cnd instanceof ElementaryCondition && cnd.isPrunable() == true) {
				ElementaryCondition ec = (ElementaryCondition)cnd;
				String attr = ec.getAttribute();
				if (shortened.containsKey(attr)) {
					ElementaryCondition old = shortened.get(attr);
					ec = old.intersect(ec);
				}
				shortened.put(attr, ec);
			} else {
				unshortened.add(cnd);
			}
		}

		// Remove conditions from first version of premise
		//this.rule.getPremise().getSubconditions().removeAll(subconditions);

		// add unshortened conditions
		for (ConditionBase cnd : unshortened) {
			newRule.getPremise().addSubcondition(cnd);
		}

		// add shortened conditions
		for (ConditionBase cnd : shortened.values()) {
			newRule.getPremise().addSubcondition(cnd);
		}
		return newRule;
	}
}
