package pl.polsl.survival.actionrules;

import java.util.ArrayList;
import java.util.List;

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

		for (RuleStatistic ruleStat: this.ruleStatistics) {
			if (ruleStat.getNumberOfActions() == 0) {
				this.numberOfRulesWithoutActionInPremise++;
			}
			this.aggregatedNumberOfConditions += ruleStat.getNumberOfConditions();
			this.aggregatedNumberOfActions += ruleStat.getNumberOfActions();
			this.aggregatedNumberOfActionsTypeANY += ruleStat.getNumberOfActionTypeANY();
		}
		this.meanNumberConditionsPerRule = (double)this.aggregatedNumberOfConditions / (double)this.numberOfRules;
		this.meanNumberOfActionsPerRule = (double)this.aggregatedNumberOfActions / (double)this.numberOfRules;
	}

	private void generateStatisticEachRule() {
		this.ruleStatistics = new ArrayList<RuleStatistic>();
		for (SurvivalActionRule rule: this.rules) {
			this.ruleStatistics.add(new RuleStatistic(rule, this.exampleSet));
		}
	}
}
