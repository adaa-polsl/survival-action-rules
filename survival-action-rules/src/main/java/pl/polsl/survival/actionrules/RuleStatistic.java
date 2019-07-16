package pl.polsl.survival.actionrules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rapidminer.example.ExampleSet;

import adaa.analytics.rules.logic.induction.Covering;
import adaa.analytics.rules.logic.representation.Action;
import adaa.analytics.rules.logic.representation.AnyValueSet;
import adaa.analytics.rules.logic.representation.CompoundCondition;
import adaa.analytics.rules.logic.representation.ConditionBase;
import adaa.analytics.rules.logic.representation.ElementaryCondition;
import adaa.analytics.rules.logic.representation.IValueSet;
import adaa.analytics.rules.logic.representation.Interval;
import adaa.analytics.rules.logic.representation.KaplanMeierEstimator;
import adaa.analytics.rules.logic.representation.SingletonSet;
import lombok.Getter;

@Getter
public class RuleStatistic {

	private static final double MEDIAN_PROB = 0.5;
	private SurvivalActionRule rule;
	private ExampleSet exampleSet;

	private int numberOfConditions = 0;
	private int numberOfActions = 0;
	private int numberOfActionTypeANY = 0;
	private double percentOfExamplesCoveredByLeftRule = 0;
	private double percentOfExamplesCoveredByRightRule = 0;
	private double pValueLogRankLeftRightRules = 0;
	private double medianSurvivalTimeLeftRule = 0;
	private double medianSurvivalTimeRightRule = 0;
	private double differenceMedianSurvivalTimeBetweenLeftRight = 0;

	private Set<Integer> coveredExLeft;
	private Set<Integer> coveredExRight;

	public RuleStatistic(SurvivalActionRule rule, ExampleSet exampleSet) {
		this.rule = rule;
		this.exampleSet = exampleSet;
		reduceActionsThisSameAttribute();
		calculateStatistics();
	}

	private void calculateStatistics() {
		this.numberOfConditions = this.rule.getLeftRule().getPremise().getSubconditions().size();
		this.numberOfActions = calculateNumberOfActions();
		this.numberOfActionTypeANY = calculateNumberOfActionsTypeANY();
		Covering leftCovering =  rule.getLeftRule().covers(this.exampleSet);
		Covering rightCovering =  rule.getRightRule().covers(this.exampleSet);
		this.coveredExLeft = leftCovering.positives;
		this.coveredExRight = rightCovering.positives;
		this.percentOfExamplesCoveredByLeftRule = ((double)this.coveredExLeft.size()) / (this.exampleSet.size()) * 100;
		this.percentOfExamplesCoveredByRightRule = ((double)this.coveredExRight.size()) / (this.exampleSet.size()) * 100;
		this.pValueLogRankLeftRightRules = this.rule.getPValueLogRank(exampleSet);
		KaplanMeierEstimator kmLeft = new KaplanMeierEstimator(this.exampleSet, leftCovering.positives);
		this.medianSurvivalTimeLeftRule = kmLeft.getTimeForProbability(MEDIAN_PROB);
		KaplanMeierEstimator kmRight = new KaplanMeierEstimator(this.exampleSet, rightCovering.positives);
		this.medianSurvivalTimeRightRule = kmRight.getTimeForProbability(MEDIAN_PROB);
		this.differenceMedianSurvivalTimeBetweenLeftRight = this.medianSurvivalTimeLeftRule - this.medianSurvivalTimeRightRule;
	}

	/**
	 * This function calculates number of actions.
	 * If left and right part of a condition is this same than it is not an action.
	 * @return Number of actions
	 */
	private int calculateNumberOfActions() {
		int n = 0;
		for (ConditionBase premiseCond: this.rule.getPremise().getSubconditions()) {
			Action action = (Action)premiseCond;
			Class<? extends IValueSet> leftValueClass = action.getLeftValue() == null ? null : action.getLeftValue().getClass();
			Class<? extends IValueSet> rightValueClass = action.getRightValue() == null ? null : action.getRightValue().getClass();

			if (rightValueClass != null && leftValueClass != null &&
					rightValueClass.equals(AnyValueSet.class) && !leftValueClass.equals(AnyValueSet.class)) {
				n++;
			} else if (leftValueClass.equals(rightValueClass)) { // it should be changed
				if (leftValueClass == SingletonSet.class) {
					SingletonSet left = (SingletonSet)action.getLeftValue();
					SingletonSet right = (SingletonSet)action.getRightValue();
					if (!left.equals(right)) {
						n++;
					}
				} else if (leftValueClass == Interval.class) {
					Interval left = (Interval)action.getLeftValue();
					Interval right = (Interval)action.getRightValue();
					if (!left.equals(right)) {
						n++;
					}
				} else {
					if (!action.getLeftCondition().equals(action.getRightCondition())) {
						n++;
					}
				}
			}
		}
		return n;
	}

	private int calculateNumberOfActionsTypeANY() {
		int n = 0;
		for (ConditionBase premiseCond: this.rule.getPremise().getSubconditions()) {
			Action action = (Action)premiseCond;
			if (action.getRightValue() instanceof AnyValueSet) {
				n++;
			}
		}
		return n;
	}

	private void reduceActionsThisSameAttribute() {
		Map<String, ElementaryCondition> shortened = new HashMap<String, ElementaryCondition>();
		Set<ConditionBase> unshortened = new HashSet<ConditionBase>();
		List<ConditionBase> subconditions = this.rule.getPremise().getSubconditions();
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
		this.rule.getPremise().getSubconditions().removeAll(subconditions);

		// add unshortened conditions
		for (ConditionBase cnd : unshortened) {
			this.rule.getPremise().addSubcondition(cnd);
		}

		// add shortened conditions
		for (ConditionBase cnd : shortened.values()) {
			this.rule.getPremise().addSubcondition(cnd);
		}
	}
}
