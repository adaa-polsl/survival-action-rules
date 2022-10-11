package pl.polsl.survival.actionrules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Partition;
import com.rapidminer.example.set.SplittedExampleSet;

import adaa.analytics.rules.logic.induction.Covering;
import adaa.analytics.rules.logic.quality.LogRank;
import adaa.analytics.rules.logic.representation.Action;
import adaa.analytics.rules.logic.representation.ActionRule;
import adaa.analytics.rules.logic.representation.AnyValueSet;
import adaa.analytics.rules.logic.representation.ConditionBase;
import adaa.analytics.rules.logic.representation.ElementaryCondition;
import adaa.analytics.rules.logic.representation.Interval;
import adaa.analytics.rules.logic.representation.KaplanMeierEstimator;
import adaa.analytics.rules.logic.representation.SingletonSet;
import adaa.analytics.rules.logic.representation.SurvivalRule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class SurvivalActionRulesGenerator {
	private int minRuleCovered;
	private TargetRulePosition targetRulePosition; // Should rules be found to make survival curves better (or worse)?
	private float maxCommonExamplesPercentage;
	private float maxRuleCoveringPercentage;
	private ExampleSet exampleSet;	
	private ExampleSet uncoveredExampleSet;
	private Set<String> stableAttributes; // Which attributes shouldn't be changed

	protected static final int DEFAULT_MIN_COVERED = 2;
	protected static final float DEFAULT_MAX_COMMON_EXAMPLES_PER = 0.1f; // the max percentage of common examples that a left and right rule can maximally share, in particular 0 - none and 1 - all
	protected static final float DEFAULT_MAX_RULE_COVERING_PERCENTAGE = 0.5f;
	protected static final Set<String> EMPTY_STABLE_ATTRIBUTES = Collections.emptySet();
	protected static final TargetRulePosition DEFAULT_INDUCE_BETTER_SURVIVAL = TargetRulePosition.BETTER;

	private SurvivalActionRulesGenerator(ExampleSet exampleSet) {
		this.minRuleCovered = DEFAULT_MIN_COVERED;
		this.targetRulePosition = DEFAULT_INDUCE_BETTER_SURVIVAL;
		this.maxCommonExamplesPercentage = DEFAULT_MAX_COMMON_EXAMPLES_PER;
		this.maxRuleCoveringPercentage = DEFAULT_MAX_RULE_COVERING_PERCENTAGE;
		this.exampleSet = exampleSet;
		this.uncoveredExampleSet = exampleSet;
		this.stableAttributes = EMPTY_STABLE_ATTRIBUTES;
	}

	// Factory methods
	public static SurvivalActionRulesGenerator createSurvivalActionRulesGenerator(ExampleSet exampleSet) {
		checkExampleSet(exampleSet, DEFAULT_MIN_COVERED);
		SurvivalActionRulesGenerator generator = new SurvivalActionRulesGenerator(exampleSet);
		return generator;
	}

	public static SurvivalActionRulesGenerator createSurvivalActionRulesGenerator(
			ExampleSet exampleSet,
			ExampleSet uncoveredExampleSet,
			int minRuleCovered,
			TargetRulePosition induceBetterSurvival,
			float maxCommonExamplesPercentage,
			float maxRuleCoveringPercentage,
			Set<String> stableAttributes) {
		checkExampleSet(exampleSet, minRuleCovered);
		checkUncoveredExampleSet(uncoveredExampleSet, exampleSet, minRuleCovered);
		checkStableAttributes(exampleSet, stableAttributes);

		SurvivalActionRulesGenerator generator = new SurvivalActionRulesGenerator(
				minRuleCovered, induceBetterSurvival, maxCommonExamplesPercentage, maxRuleCoveringPercentage, exampleSet, uncoveredExampleSet, stableAttributes);
		return generator;
	}

	// Setters
	public void setExampleSet(ExampleSet exampleSet) {
		checkExampleSet(exampleSet, DEFAULT_MIN_COVERED);
		this.exampleSet = exampleSet;
	}
	public void setUncoveredExampleSet(ExampleSet uncoveredExampleSet) {
		checkUncoveredExampleSet(uncoveredExampleSet, this.exampleSet, this.minRuleCovered);
		this.uncoveredExampleSet = uncoveredExampleSet;
	}
	public void setMinRuleCovered(int minRuleCovered) {
		if (minRuleCovered < 1 | minRuleCovered >= this.exampleSet.size()) {
			throw new IllegalArgumentException("Minimum number of rules must be positive value." +
					" It can't be equal or bigger than number of examples in exampleSet.");
		}
		this.minRuleCovered = minRuleCovered;
	}

	public void setMaxCommonExamplesPercentage(float maxCommonExamplesPercentage) {
		if (maxCommonExamplesPercentage > 1F & maxCommonExamplesPercentage < 0F) {
			throw new IllegalArgumentException("Maximum percentage of common examples must be between 0 and 1.");
		}
		this.maxCommonExamplesPercentage = maxCommonExamplesPercentage;
	}
	public void setMaxRuleCoveringPercentage(float maxRuleCoveringPercentage) {
		if (maxRuleCoveringPercentage > 1F & maxRuleCoveringPercentage < 0F) {
			throw new IllegalArgumentException("Maximum rule coverage must be between 0 and 1.");
		}
		this.maxRuleCoveringPercentage = maxRuleCoveringPercentage;
	}
	public void setStableAttributes(Set<String> stableAttributes) {
		checkStableAttributes(this.exampleSet, stableAttributes);
		this.stableAttributes = stableAttributes;
	}

	// Proper methods

	public List<SurvivalActionRule> induceSurvivalActionRuleList() {
		List<SurvivalActionRule> survivalActionRuleSet = new ArrayList<SurvivalActionRule>();
		while (this.uncoveredExampleSet.size() != 0) {
			SurvivalActionRule survActionRule = this.growRule();
			// Do not add empty action rule to returned set
			if (survActionRule.getPremise().getSubconditions().size() > 0) {
				System.out.println("Before pruning: " + survActionRule);
				survActionRule = this.pruneRule(survActionRule);
				survivalActionRuleSet.add(survActionRule);
			}
			Covering covering = survActionRule.covers(this.uncoveredExampleSet); // empty rule covers all examples
			System.out.println("Rule coveres " + covering.positives.size() + " uncovered examples.");
			System.out.println("Number of rules: " + survivalActionRuleSet.size());
			// Delete covered examples
			this.uncoveredExampleSet = removeExamplesFromExampleSet(this.uncoveredExampleSet, covering.positives);
		}
		return survivalActionRuleSet;
	}

	private ExampleSet removeExamplesFromExampleSet(ExampleSet exampleSet, Set<Integer> examplesIdsToRemove) {
		int[] elements = new int[uncoveredExampleSet.size()];
		for (int i: examplesIdsToRemove) {
			elements[i] = 1;
		}
		SplittedExampleSet splitted = new SplittedExampleSet(exampleSet, new Partition(elements, 2));
		splitted.selectSingleSubset(0);
		exampleSet = (ExampleSet)splitted;
		return exampleSet;
	}

	protected SurvivalActionRule pruneRule(SurvivalActionRule rule) {
		ConditionBase actionRemove;
		int nbOfExamples  = this.exampleSet.size();
		
		if (rule.getPremise().getSubconditions().size() <= 1) {
			return rule;
		}

		do {
			actionRemove = null;
			boolean loosenRightRule = false;
			double currentLogRank = rule.getLogRankValueLeftRightRule(this.exampleSet);

			List<ConditionBase> actionsInPremise = new ArrayList<ConditionBase>(rule.getPremise().getSubconditions());

			for (ConditionBase action: actionsInPremise) {
				rule.getPremise().getSubconditions().remove(action);
				double newLogRank = rule.getLogRankValueLeftRightRule(this.exampleSet);
				float commonExamplesPercentage = rule.getCommonExamplesPercentage(this.exampleSet);
				float ruleCoveringPercentage = rule.getRuleCoveringPercentage(this.exampleSet);
				SurvivalActionRule shortenedRuleAfterRemoving = rule.reduceActionsThisSameAttribute();
				int nbEmptyActionsAfterRemoving = calculateNbEmptyActions(shortenedRuleAfterRemoving);
				
				if (newLogRank >= currentLogRank &&
						nbEmptyActionsAfterRemoving != shortenedRuleAfterRemoving.getPremise().getSubconditions().size() &&
						isFulfilledTargetRulePosition(rule) && 
						ruleCoveringPercentage <= this.maxRuleCoveringPercentage &&
						commonExamplesPercentage <= this.maxCommonExamplesPercentage) {
					actionRemove = action;
					currentLogRank = newLogRank;
					loosenRightRule = false; //? should it be here?
				}
				// Try not to remove whole action but only elementary condition for right rule
				// It can be done only for conditions for attributes that are not stable attributes.
				Action act = (Action)action;
				Action emptyAction = createEmptyAction(act);
				rule.getPremise().addSubcondition(emptyAction);
				newLogRank = rule.getLogRankValueLeftRightRule(this.exampleSet);
				SurvivalActionRule shortenedRuleWithEmptyAct = rule.reduceActionsThisSameAttribute();
				int nbEmptyActionsWithEmptyAct = calculateNbEmptyActions(shortenedRuleWithEmptyAct);
				
				commonExamplesPercentage = rule.getCommonExamplesPercentage(this.exampleSet);
				ruleCoveringPercentage = rule.getRuleCoveringPercentage(this.exampleSet);				
				if (act.getRightValue().getClass() != AnyValueSet.class &&
						nbEmptyActionsWithEmptyAct != shortenedRuleWithEmptyAct.getPremise().getSubconditions().size() && // check if we wouldn't remove the last not empty action
						newLogRank >= currentLogRank &&
						!stableAttributes.contains(act.getAttribute()) &&
						isFulfilledTargetRulePosition(rule) &&
						ruleCoveringPercentage <= this.maxRuleCoveringPercentage &&
						commonExamplesPercentage <= this.maxCommonExamplesPercentage) {
					actionRemove = action;
					currentLogRank = newLogRank;
					loosenRightRule = true;
				}
				// clean - add removed action back to the rule
				rule.getPremise().removeSubcondition(emptyAction);
				rule.getPremise().addSubcondition(action);
				System.out.print("");
			}
			rule.getPremise().removeSubcondition(actionRemove); // delete whole action
			if (loosenRightRule) {								// if needed add this same action but with ANY as right condition
				Action act = (Action)actionRemove;
				Action emptyAction = createEmptyAction(act);
				rule.getPremise().addSubcondition(emptyAction);
			}
		} while(rule.getPremise().getSubconditions().size() > 0 &&
				actionRemove != null &&
				rule.getLeftRule().covers(this.exampleSet).getSize() != nbOfExamples &&
				rule.getRightRule().covers(this.exampleSet).getSize() != nbOfExamples);

		return rule;
	}

	private int calculateNbEmptyActions(ActionRule rule) {
		int nbEmptyActions = 0;
		
		for (ConditionBase cb: rule.getPremise().getSubconditions()) {
			Action ac = (Action)cb;
			if (ac.getRightValue() != null &&
					ac.getRightValue().getClass() == AnyValueSet.class) {
				nbEmptyActions++;
			}
		}
		return nbEmptyActions;
	}
	
	private Action createEmptyAction(Action currentAction) {
		Action emptyAction = new Action((ElementaryCondition)currentAction.getLeftCondition(), 
				new ElementaryCondition(currentAction.getAttribute(), new AnyValueSet()));
		return emptyAction;
	}

	protected SurvivalActionRule growRule() {
		SurvivalActionRule actionRule = new SurvivalActionRule(this.exampleSet.getAttributes().getLabel().getName());
		ElementaryCondition bestConditionLeftRule = null;
		ElementaryCondition bestConditionRightRule = null;
		Set<ElementaryCondition> checkedConditionsLeftRule = new HashSet<ElementaryCondition>();
		do {
			bestConditionLeftRule = (ElementaryCondition)getBestElementaryCondition(actionRule.getLeftRule(),
					checkedConditionsLeftRule);
			checkedConditionsLeftRule.add(bestConditionLeftRule);
			if (bestConditionLeftRule == null) {
				continue;
			}
			SurvivalRule leftRule = actionRule.getLeftRule();
			leftRule.getPremise().addSubcondition(bestConditionLeftRule);
			bestConditionRightRule = (ElementaryCondition)findCounterCondition(bestConditionLeftRule, 
					leftRule, actionRule.getRightRule());
			if (bestConditionRightRule == null) {
				continue;
			}
			Action newAction = new Action(bestConditionLeftRule, bestConditionRightRule);
			actionRule.getPremise().addSubcondition(newAction);
		} while(bestConditionLeftRule != null);
		return actionRule;
	}
	
	private float getCommonExamplesPercentage(SurvivalRule leftRule, SurvivalRule rightRule)
	{
		Covering leftRuleCovering = leftRule.covers(this.exampleSet);
		Set<Integer> intersection = new HashSet<Integer>(leftRuleCovering.positives);
		Covering rightRuleCovering = rightRule.covers(this.exampleSet);
		intersection.retainAll(rightRuleCovering.positives);
		float commonExamplesPercentage = (float) intersection.size()/this.exampleSet.size();

		return commonExamplesPercentage;
	}

	protected ConditionBase findCounterCondition(ElementaryCondition sourceCondition, SurvivalRule leftRule, SurvivalRule rightRule) {
		ElementaryCondition bestCounterCondition = null;
		double bestLogRankValue = Double.NEGATIVE_INFINITY;
		String attribute = sourceCondition.getAttribute();

		Set<ElementaryCondition> conditionsForAttribute = new HashSet<ElementaryCondition>();
		// if this condition is for stable attribute only this same condition can be evaluated
		if (this.stableAttributes.contains(attribute)) {
			conditionsForAttribute.add(sourceCondition);
		} else {
			// get elementary conditions for this attribute only possible for examples
			// covered already by right rule
			Covering rightRuleCurrentCovering = rightRule.covers(this.exampleSet);
			getElementaryConditionForAttribute(this.exampleSet, rightRuleCurrentCovering.positives,
					conditionsForAttribute, attribute);
		}

		Covering leftRuleCovering = leftRule.covers(this.exampleSet);
		KaplanMeierEstimator leftRuleKM = new KaplanMeierEstimator(this.exampleSet, leftRuleCovering.positives);

		for (ElementaryCondition condition: conditionsForAttribute) {

			rightRule.getPremise().addSubcondition(condition);

			Covering newRightRuleCovering = rightRule.covers(this.exampleSet);
			if (newRightRuleCovering.positives.size() < this.minRuleCovered) {
				rightRule.getPremise().removeSubcondition(condition);
				continue;
			}

			float commonExamplesPercentage = this.getCommonExamplesPercentage(leftRule, rightRule);
			if (commonExamplesPercentage > this.maxCommonExamplesPercentage) {
				rightRule.getPremise().removeSubcondition(condition);
				continue;
			}

			KaplanMeierEstimator newRightRuleKM = new KaplanMeierEstimator(this.exampleSet, newRightRuleCovering.positives);
			LogRank logrank = new LogRank();
			com.rapidminer.tools.container.Pair<Double, Double> logrankResult = logrank.calculate(leftRuleKM, newRightRuleKM);

			if(newRightRuleCovering.positives.size() >= this.minRuleCovered &&
					logrankResult.getFirst() > bestLogRankValue &&
					isFulfilledTargetRulePosition(newRightRuleKM, leftRuleKM, this.targetRulePosition)) {
				bestLogRankValue = logrankResult.getFirst();
				bestCounterCondition = condition;
			}

			// clean object
			rightRule.getPremise().removeSubcondition(condition);
		}
		return bestCounterCondition;
	}

	private boolean isFulfilledTargetRulePosition(SurvivalActionRule rule) {
		SurvivalRule left = rule.getLeftRule();
		SurvivalRule right = rule.getRightRule();
		Covering coveringLeft = left.covers(this.exampleSet);
		Covering coveringRight = right.covers(this.exampleSet);
		KaplanMeierEstimator leftRuleKM = new KaplanMeierEstimator(this.exampleSet, coveringLeft.positives);
		KaplanMeierEstimator rightRuleKM = new KaplanMeierEstimator(this.exampleSet, coveringRight.positives);
		return isFulfilledTargetRulePosition(rightRuleKM, leftRuleKM, this.targetRulePosition);
	}

	protected boolean isFulfilledTargetRulePosition(KaplanMeierEstimator checkedKM,
			KaplanMeierEstimator referencialKM, TargetRulePosition targetRulePosition) {

		if (targetRulePosition == TargetRulePosition.ANY) {
			return true;
		}

		// Check value for middle time of observation
		Attribute time = this.exampleSet.getAttributes().get(SurvivalRule.SURVIVAL_TIME_ROLE);
		List<Double> timeValues = getValuesOfNumericalAttribute(time);

		double middleTime = Collections.max(timeValues) / 2.0;
		double checkedMiddleProbability = checkedKM.getProbabilityAt(middleTime);
		double referencialMiddleProbability = referencialKM.getProbabilityAt(middleTime);
		double checkAreaUnderKMPlot = calculateAreaUnderKMPlot(checkedKM);
		double referencialAreaUnderKMPlot = calculateAreaUnderKMPlot(referencialKM);

		if (targetRulePosition == TargetRulePosition.BETTER &&
				checkedMiddleProbability > referencialMiddleProbability &&
				checkAreaUnderKMPlot > referencialAreaUnderKMPlot) {
			return true;
		}
		if (targetRulePosition == TargetRulePosition.WORSE &&
				checkedMiddleProbability < referencialMiddleProbability &&
				checkAreaUnderKMPlot < referencialAreaUnderKMPlot) {
			return true;
		}
		return false;
	}

	private List<Double> getValuesOfNumericalAttribute(Attribute attr) {
		if (attr.isNumerical()) {
			List<Double> values = new ArrayList<Double>();
			for (Example ex : this.exampleSet.getExampleTable().createExampleSet()) {
				double val = ex.getValue(attr);
				values.add(val);
			}
			return	values;
		}
		return null;
	}

	protected double calculateAreaUnderKMPlot(KaplanMeierEstimator km) {
		double area = 0.0;
		List<Double> times = km.getTimes();
		for (int i = 0; i < times.size(); i++) {
			if (i == 0) {
				area += times.get(i); //Area from time 0 to first known value is always 1.0 * time, because probability is equal to 1
			}
			else {
				area += (times.get(i) - times.get(i-1)) * km.getProbabilityAt(times.get(i-1));
			}
			// for last value, calculate area to end of observations time
			if (i+1 == times.size()) {
				List<Double> timesEntireSet = getValuesOfNumericalAttribute(this.exampleSet.getAttributes().get(SurvivalRule.SURVIVAL_TIME_ROLE));
				Double maxTime = Collections.max(timesEntireSet);
				area += (maxTime - times.get(i)) * km.getProbabilityAt(times.get(i));
			}
		}
		return area;
	}

	protected ConditionBase getBestElementaryCondition(SurvivalRule currentRule, 
			Set<ElementaryCondition> conditionsNotToBeChecked) {

		ElementaryCondition bestCondition = null;
		double bestLogRankValue = Double.NEGATIVE_INFINITY;
		double nbUncoveredExamplesRuleCoveres = 0;

		// In checked conditions will be only conditions that are possible for existing rule
		Set<Attribute> normalAttributes = getNormalAttributes();
		// We will only get elementary conditions that are in uncovered set for current rule
		// because minCov must be satisfied for uncovered set
		Covering currentCov = currentRule.covers(this.uncoveredExampleSet);
		Set<ElementaryCondition> allConditions = generateElementaryConditions(this.uncoveredExampleSet, normalAttributes,
				currentCov.positives);

		// Remove conditions that were evaluated before. Mostly it will be conditions that are already in premise.
		allConditions.removeAll(currentRule.getPremise().getSubconditions());
		if (conditionsNotToBeChecked != null) {
			allConditions.removeAll(conditionsNotToBeChecked);
		}

		// Evaluate conditions
		for (ElementaryCondition condition: allConditions) {	

			currentRule.getPremise().getSubconditions().add(condition);

			// Covering for yet uncovered example set
			Covering coveringForUncoveredSet = currentRule.covers(this.uncoveredExampleSet);
			if (coveringForUncoveredSet.positives.size() < this.minRuleCovered) {
				currentRule.getPremise().getSubconditions().remove(condition); // cleaning
				continue;
			}

			// Covering for entire set and KM estimator for covered by new rule examples
			Covering coveringEntireSet = currentRule.covers(this.exampleSet);
			float currentRuleCoveringPercentage = (float) currentRule.covers(this.exampleSet).positives.size()/this.exampleSet.size();
			if (currentRuleCoveringPercentage > this.maxRuleCoveringPercentage) {
				currentRule.getPremise().getSubconditions().remove(condition); // cleaning
				continue;
			}
			KaplanMeierEstimator kmCoveredByRule = new KaplanMeierEstimator(this.exampleSet, coveringEntireSet.positives);

			// KM for examples uncovered by new rule
			Set<Integer> uncoveredExamplesIds = getComplementOfIdsSet(coveringEntireSet.positives, this.exampleSet.size());
			KaplanMeierEstimator kmUncoveredByRule = new KaplanMeierEstimator(this.exampleSet, uncoveredExamplesIds);

			LogRank logrank = new LogRank();
			com.rapidminer.tools.container.Pair<Double, Double> logrankResult = logrank.calculate(kmCoveredByRule, kmUncoveredByRule);

			if(coveringForUncoveredSet.positives.size() >= this.minRuleCovered &&
					(logrankResult.getFirst() > bestLogRankValue ||
							(logrankResult.getFirst() == bestLogRankValue && 
							coveringForUncoveredSet.positives.size() > nbUncoveredExamplesRuleCoveres))) {
				bestLogRankValue = logrankResult.getFirst();
				bestCondition = condition;
				nbUncoveredExamplesRuleCoveres = coveringForUncoveredSet.positives.size();
			}

			// clean object
			currentRule.getPremise().getSubconditions().remove(condition);
		}
		return bestCondition;
	}

	private Set<Integer> getComplementOfIdsSet(Set<Integer> ids, int sizeOfSet) {
		Set<Integer> complementOfSet = new HashSet<Integer>();
		for(int i = 0; i < sizeOfSet; i++) {
			if (!ids.contains(i)) {
				complementOfSet.add(i);
			}
		}
		return complementOfSet;
	}

	protected Set<ElementaryCondition> generateElementaryConditions(
			ExampleSet trainSet,
			Set<Attribute> allowedAttributes,
			Set<Integer> coveredByRule) {

		HashSet<ElementaryCondition> conditions = new HashSet<ElementaryCondition>();

		for (Attribute attr : allowedAttributes) {
			getElementaryConditionForAttribute(trainSet, coveredByRule, conditions, attr.getName());
		}
		return conditions;
	}

	private void getElementaryConditionForAttribute(
			ExampleSet trainSet,
			Set<Integer> coveredByRule,
			Set<ElementaryCondition> conditions,
			String attributeName) {

		Attribute attribute = trainSet.getAttributes().get(attributeName);
		Set<Double> attributeValues = new HashSet<Double>();

		// Get only examples that are covered by rule
		// It will make less conditions to evaluate
		for (int id : coveredByRule) {
			Example ex = trainSet.getExample(id);
			double value = ex.getValue(attribute);
			attributeValues.add(value);
		}

		if (attribute.isNominal()) {
			for (double val : attributeValues) {
				conditions.add(
						new ElementaryCondition(
								attributeName, 
								new SingletonSet(val, attribute.getMapping().getValues())));
			}
		} else {
			//numerical attribute - have to find midpoints
			// Change to list in order to sort it
			List<Double> attributeValuesList = new ArrayList<Double>(attributeValues);
			Collections.sort(attributeValuesList);

			for(int i = 0; i <  attributeValuesList.size()-1; i++) {
				Double midPoint = (attributeValuesList.get(i) + attributeValuesList.get(i+1)) / 2.0;
				conditions.add(new ElementaryCondition(attributeName, Interval.create_le(midPoint)));
				conditions.add(new ElementaryCondition(attributeName, Interval.create_geq(midPoint)));
			}
		}
	}

	protected Set<Attribute> getAttributesAllowedToChange() {
		Set<Attribute> allowedAttributes = new HashSet<Attribute>();
		Set<Attribute> normalAttributes = getNormalAttributes();

		for (Attribute attr: normalAttributes) {
			if (!this.stableAttributes.contains(attr.getName())) {
				allowedAttributes.add(attr);
			}
		}
		return allowedAttributes;		
	}

	protected Set<Attribute> getNormalAttributes() {
		Attributes allAttributes = this.exampleSet.getAttributes();
		Set<Attribute> normalAttributes = new HashSet<Attribute>();

		Attribute survivalStatusAttr = allAttributes.getSpecial(SurvivalRule.SURVIVAL_TIME_ROLE);
		for (Attribute attr: allAttributes) {
			if (attr != allAttributes.getLabel() &&
					attr != survivalStatusAttr) {
				normalAttributes.add(attr);
			}
		}
		return normalAttributes;
	}

	public static void checkExampleSet(ExampleSet exampleSet, int minRuleCovered) {
		if (exampleSet.getExampleTable().getNumberOfAttributes() < 3 |
				exampleSet.getAttributes().getLabel() == null |
				exampleSet.getAttributes().get(SurvivalRule.SURVIVAL_TIME_ROLE) == null |
				exampleSet.size() < minRuleCovered) {
			throw new IllegalArgumentException("ExampleSet is not correct. " +
					"Check number of columns and rows." +
					"Check also if columns for survival time and survival status exsist.");
		}
	}

	public static void checkUncoveredExampleSet(ExampleSet uncoveredExampleSet, ExampleSet exampleSet, int minRuleCovered) {
		SurvivalActionRulesGenerator.checkExampleSet(uncoveredExampleSet, minRuleCovered);

		// Check if uncoveredExampleSet is subset from exampleSet
		if (!exampleSet.getAttributes().equals(uncoveredExampleSet.getAttributes())) {

			for(Example uex: uncoveredExampleSet) {
				boolean foundExample = false;
				Iterator<Example> reader = exampleSet.iterator();
				while (reader.hasNext()) {
					Example example = reader.next();
					boolean thisSameAttributes = true;
					for (Attribute attr: example.getAttributes()) {
						if(!example.getValueAsString(attr).equals(uex.getValueAsString(attr))) {
							thisSameAttributes = false;
						}
					}
					if (thisSameAttributes) {
						foundExample = true;
						break;
					}
				}

				if (foundExample == false) {
					throw new IllegalArgumentException("Example " +
							uex +
							" from uncovered example set does not occure in example set.");
				}
			}
		}
	}

	public static void checkStableAttributes(ExampleSet exampleSet, Set<String> stableAttributes) {
		for(String attr: stableAttributes) {
			if (exampleSet.getAttributes().get(attr) == null) {
				throw new IllegalArgumentException("Attribute" + attr +
						" from list of stable attributes does not occure in example set.");
			}
		}
	}
}
