package pl.polsl.survival.actionrules;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import com.rapidminer.example.ExampleSet;

import lombok.Getter;

@Getter
public class RulesSetStatistics {
	private List<SurvivalActionRule> rules;
	private ExampleSet exampleSet;
	private List<RuleStatistic> ruleStatistics;

	private int numberOfRules = 0;
	private int numberOfRulesWithoutActionInPremise = 0;
	private int aggregatedNumberOfConditions = 0;
	private double meanNumberConditionsPerRule = 0;
	private int aggregatedNumberOfActions = 0;
	private int aggregatedNumberOfActionsTypeANY = 0;
	private double meanNumberOfActionsPerRule = 0;

	private double sumPercentOfExamplesCoveredByLeftAndRightRules = 0;
	private double sumPercentOfExamplesCoveredByLeftRule = 0;
	private double sumPercentOfExamplesCoveredByRightRule = 0;

	private double meanPercentOfExamplesCoveredByLeftAndRightRules = 0;
	private double meanPercentOfExamplesCoveredByLeftRule = 0;
	private double meanPercentOfExamplesCoveredByRightRule = 0;

	private double percentOfExamplesCoveredByLeftRule = 0;
	private double percentOfExamplesCoveredByRightRule = 0;
	private double percentOfExamplesCoveredByLeftAndRightRules = 0;
	private double percentOfExamplesCoveredByLeftOrRightRules = 0;

	private Set<Integer> coveredExLeft = new HashSet<Integer>();
	private Set<Integer> coveredExRight = new HashSet<Integer>();

	public RulesSetStatistics(List<SurvivalActionRule> rules, ExampleSet exampleSet) {
		this.rules = rules;
		this.exampleSet = exampleSet;
		generateStatisticEachRule();
		calculateStatistics();
	}

	private void calculateStatistics() {
		this.numberOfRules = rules.size();

		this.numberOfRulesWithoutActionInPremise = 0;
		this.aggregatedNumberOfConditions = 0;
		this.meanNumberConditionsPerRule = 0;
		this.aggregatedNumberOfActions = 0;
		this.aggregatedNumberOfActionsTypeANY = 0;
		this.meanNumberOfActionsPerRule = 0;
		this.sumPercentOfExamplesCoveredByLeftAndRightRules = 0;
		this.sumPercentOfExamplesCoveredByLeftRule = 0;
		this.sumPercentOfExamplesCoveredByRightRule = 0;
		this.percentOfExamplesCoveredByLeftRule = 0;
		this.percentOfExamplesCoveredByRightRule = 0;

		for (RuleStatistic ruleStat: this.ruleStatistics) {
			if (ruleStat.getNumberOfActions() == 0) {
				this.numberOfRulesWithoutActionInPremise++;
			}
			this.aggregatedNumberOfConditions += ruleStat.getNumberOfConditions();
			this.aggregatedNumberOfActions += ruleStat.getNumberOfActions();
			this.aggregatedNumberOfActionsTypeANY += ruleStat.getNumberOfActionTypeANY();
			this.sumPercentOfExamplesCoveredByLeftAndRightRules += ruleStat.getPercentOfExamplesCoveredByLeftAndRightRules();
			this.sumPercentOfExamplesCoveredByLeftRule += ruleStat.getPercentOfExamplesCoveredByLeftRule();
			this.sumPercentOfExamplesCoveredByRightRule += ruleStat.getPercentOfExamplesCoveredByRightRule();

			this.coveredExLeft.addAll(ruleStat.getCoveredExLeft());
			this.coveredExRight.addAll(ruleStat.getCoveredExRight());
		}
		this.meanNumberConditionsPerRule = (double)this.aggregatedNumberOfConditions / (double)this.numberOfRules;
		this.meanNumberOfActionsPerRule = (double)this.aggregatedNumberOfActions / (double)this.numberOfRules;
		this.meanPercentOfExamplesCoveredByLeftAndRightRules = (double)this.sumPercentOfExamplesCoveredByLeftAndRightRules / this.numberOfRules;
		this.meanPercentOfExamplesCoveredByLeftRule = (double)this.sumPercentOfExamplesCoveredByLeftRule / this.numberOfRules;
		this.meanPercentOfExamplesCoveredByRightRule = (double)this.sumPercentOfExamplesCoveredByRightRule / this.numberOfRules;

		this.percentOfExamplesCoveredByLeftRule =  100 * (double)this.coveredExLeft.size() / this.exampleSet.size();
		this.percentOfExamplesCoveredByRightRule = 100 * (double)this.coveredExRight.size() / this.exampleSet.size();
		Set<Integer> coveredExLeftAndRight = new HashSet<Integer>(this.coveredExLeft);
		coveredExLeftAndRight.retainAll(coveredExRight);
		this.percentOfExamplesCoveredByLeftAndRightRules = 100 * (double)coveredExLeftAndRight.size() / this.exampleSet.size();
		Set<Integer> coveredExLeftOrRight = new HashSet<Integer>(this.coveredExLeft);
		coveredExLeftOrRight.addAll(coveredExRight);
		this.percentOfExamplesCoveredByLeftOrRightRules = 100 * (double)coveredExLeftOrRight.size() / this.exampleSet.size();
		
	}

	private void generateStatisticEachRule() {
		this.ruleStatistics = new ArrayList<RuleStatistic>();
		for (SurvivalActionRule rule: this.rules) {
			this.ruleStatistics.add(new RuleStatistic(rule, this.exampleSet));
		}
	}
}
