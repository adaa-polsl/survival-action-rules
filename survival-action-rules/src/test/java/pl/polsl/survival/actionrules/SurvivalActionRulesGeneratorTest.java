package pl.polsl.survival.actionrules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Partition;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Ontology;

import adaa.analytics.rules.logic.induction.Covering;
import adaa.analytics.rules.logic.representation.Action;
import adaa.analytics.rules.logic.representation.CompoundCondition;
import adaa.analytics.rules.logic.representation.ConditionBase;
import adaa.analytics.rules.logic.representation.ElementaryCondition;
import adaa.analytics.rules.logic.representation.Interval;
import adaa.analytics.rules.logic.representation.KaplanMeierEstimator;
import adaa.analytics.rules.logic.representation.SingletonSet;
import adaa.analytics.rules.logic.representation.SurvivalRule;


public class SurvivalActionRulesGeneratorTest {
	private static ExampleSet exampleSet;
	private SurvivalActionRulesGenerator generator;

	private static List<String> mappingForAttrD;

	@BeforeClass
	public static void init() throws OperatorException, OperatorCreationException {
		exampleSet = TestUtils.prepareExampleSet();
		mappingForAttrD = exampleSet.getAttributes().get("d").getMapping().getValues();
	}

	@Before
	public void createGenerator() {
		this.generator = SurvivalActionRulesGenerator.createSurvivalActionRulesGenerator(exampleSet);
	}

	@Test
	public void createDefault() {
		// Given

		// When
		SurvivalActionRulesGenerator defaultGenerator =
				SurvivalActionRulesGenerator.createSurvivalActionRulesGenerator(exampleSet);

		// Then
		assertEquals(exampleSet, defaultGenerator.getExampleSet());
		assertEquals(exampleSet, defaultGenerator.getUncoveredExampleSet());
		assertEquals(0, defaultGenerator.getStableAttributes().size());
	}

	@Test
	public void induceSurvivalActionRuleList_5MinRuleCovered() {
		// Given
		this.generator.setMinRuleCovered(5);
		Action action = new Action(
				new ElementaryCondition("a", new Interval(Interval.MINUS_INF, 9.5, false, false)), 
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)));
		CompoundCondition premise = new CompoundCondition();
		premise.addSubcondition(action);
		SurvivalActionRule actionRule = new SurvivalActionRule(premise, SurvivalActionRule.createConsequence("survival_status"));
		List<SurvivalActionRule> expectedSet = new ArrayList<SurvivalActionRule>();
		expectedSet.add(actionRule);

		// When
		List<SurvivalActionRule> actualSet = this.generator.induceSurvivalActionRuleList();

		// Then
		assertEquals(expectedSet, actualSet);
	}

	@Test
	public void induceSurvivalActionRuleList_8MinRuleCovered() {
		// Given
		this.generator.setMinRuleCovered(8);
		Action action = new Action(
				new ElementaryCondition("a", new Interval(Interval.MINUS_INF, 9.5, false, false)), 
				new ElementaryCondition("a", new Interval(2.5, Interval.INF, true, false)));
		CompoundCondition premise = new CompoundCondition();
		premise.addSubcondition(action);
		SurvivalActionRule actionRule = new SurvivalActionRule(premise, SurvivalActionRule.createConsequence("survival_status"));
		List<SurvivalActionRule> expectedSet = new ArrayList<SurvivalActionRule>();
		expectedSet.add(actionRule);

		// When
		List<SurvivalActionRule> actualSet = this.generator.induceSurvivalActionRuleList();

		// Then
		assertEquals(expectedSet, actualSet);
	}

	@Test
	public void induceSurvivalActionRuleList_9MinRuleCovered() {
		// Given
		this.generator.setMinRuleCovered(9);
		Action action = new Action(
				new ElementaryCondition("a", new Interval(Interval.MINUS_INF, 9.5, false, false)), 
				new ElementaryCondition("a", new Interval(1.5, Interval.INF, true, false)));
		CompoundCondition premise = new CompoundCondition();
		premise.addSubcondition(action);
		SurvivalActionRule actionRule = new SurvivalActionRule(premise, SurvivalActionRule.createConsequence("survival_status"));
		List<SurvivalActionRule> expectedSet = new ArrayList<SurvivalActionRule>();
		expectedSet.add(actionRule);

		// When
		List<SurvivalActionRule> actualSet = this.generator.induceSurvivalActionRuleList();

		// Then
		assertEquals(expectedSet, actualSet);
	}

	@Test
	public void pruneRule() {
		// Given
		Action action1 = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(8.5, Interval.INF, true, false)));
		Action action2 = new Action(
				new ElementaryCondition("a", new Interval(4.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(6.5, Interval.INF, true, false)));
		CompoundCondition inputPremise = new CompoundCondition();
		inputPremise.addSubcondition(action1);
		inputPremise.addSubcondition(action2);
		SurvivalActionRule actionRule = new SurvivalActionRule(inputPremise, SurvivalActionRule.createConsequence("survival_status"));

		CompoundCondition outputPremise = new CompoundCondition();
		outputPremise.addSubcondition(action1);
		outputPremise.addSubcondition(action2);
		SurvivalActionRule expectedRule = new SurvivalActionRule(outputPremise, SurvivalActionRule.createConsequence("survival_status"));

		// When
		SurvivalActionRule prunedRule = this.generator.pruneRule(actionRule);

		// Then
		assertEquals(expectedRule, prunedRule);
	}

	@Test
	public void growRule_wholeSetUncovered() {
		// Given
		Action action1 = new Action(
				new ElementaryCondition("b", new Interval(37.5, Interval.INF, true, false)), 
				new ElementaryCondition("b", new Interval(17.5, Interval.INF, true, false)));
		Action action2 = new Action(
				new ElementaryCondition("d", new SingletonSet(0.0, mappingForAttrD)), 
				new ElementaryCondition("d", new SingletonSet(1.0, mappingForAttrD)));
		CompoundCondition premise = new CompoundCondition();
		premise.addSubcondition(action1);
		premise.addSubcondition(action2);
		SurvivalActionRule expectedActionRule = new SurvivalActionRule(premise, SurvivalActionRule.createConsequence("survival_status"));

		// When
		SurvivalActionRule actionRule = this.generator.growRule();

		// Then
		assertEquals(expectedActionRule, actionRule);
	}

	@Test
	public void getAttributesAllowedToChange() {
		// Given
		Set<String> stableAttributes = new HashSet<String>(Arrays.asList("b", "d"));
		Set<Attribute> expectedAttributes = new HashSet<Attribute>(Arrays.asList(exampleSet.getAttributes().get("a"), 
				exampleSet.getAttributes().get("c")));
		this.generator.setStableAttributes(stableAttributes);

		// When
		Set<Attribute> allowedAttributes = this.generator.getAttributesAllowedToChange();

		// Then
		assertEquals(expectedAttributes, allowedAttributes);
	}

	@Test
	public void getNormalAttributes() {
		// Given
		Set<String> stableAttributes = new HashSet<String>(Arrays.asList("b", "d"));
		Set<Attribute> expectedAttributes = new HashSet<Attribute>(Arrays.asList(
				exampleSet.getAttributes().get("a"), 
				exampleSet.getAttributes().get("b"),
				exampleSet.getAttributes().get("c"),
				exampleSet.getAttributes().get("d")));
		this.generator.setStableAttributes(stableAttributes);

		// When
		Set<Attribute> normalAttributes = this.generator.getNormalAttributes();

		// Then
		assertEquals(expectedAttributes, normalAttributes);
	}

	@Test
	public void getElementaryConditions_forWholeSet() {
		// Given
		Set<String> stableAttributes = new HashSet<String>(Arrays.asList("a", "c"));
		this.generator.setStableAttributes(stableAttributes);
		Set<Attribute> allowedAttributes = this.generator.getAttributesAllowedToChange();
		Set<Integer> ids = new HashSet<Integer>();
		for (int i=0; i<10; i++) {
			ids.add(i);
		}

		// When 
		Set<ElementaryCondition> elementaryConditions = this.generator.generateElementaryConditions(exampleSet, allowedAttributes, ids);

		//Then
		// 2 elementary conditions for attribute "d", 18 elementary conditions for attribute "b"
		assertEquals(20, elementaryConditions.size()); 
		assertTrue(elementaryConditions.contains(new ElementaryCondition("b", new Interval(47.5, Interval.INF, true, false))));
		assertTrue(elementaryConditions.contains(new ElementaryCondition("b", new Interval(Interval.MINUS_INF, 47.5, false, false))));
		assertTrue(elementaryConditions.contains(new ElementaryCondition("b", new Interval(7.5, Interval.INF, true, false))));
		assertTrue(elementaryConditions.contains(new ElementaryCondition("b", new Interval(Interval.MINUS_INF, 7.5, false, false))));
		assertTrue(elementaryConditions.contains(new ElementaryCondition("d", new SingletonSet(0.0, mappingForAttrD))));
		assertTrue(elementaryConditions.contains(new ElementaryCondition("d", new SingletonSet(1.0, mappingForAttrD))));
	}

	@Test
	public void getElementaryConditions_reapetedNumericalValues() {
		// Given
		this.generator.setStableAttributes(Collections.EMPTY_SET);
		Set<Attribute> allowedAttributes = new HashSet<Attribute>();
		//Create new example set
		List<Attribute> attributes = new LinkedList<>();
		attributes.add(AttributeFactory.createAttribute("num", Ontology.NUMERICAL));
		MemoryExampleTable table = new MemoryExampleTable(attributes);
		double[] data = {12.5};
		double[] data2 = {14.5};
		double[] data3 = {9.5};
		table.addDataRow(new DoubleArrayDataRow(data));
		table.addDataRow(new DoubleArrayDataRow(data2));
		table.addDataRow(new DoubleArrayDataRow(data));
		table.addDataRow(new DoubleArrayDataRow(data));
		table.addDataRow(new DoubleArrayDataRow(data2));
		table.addDataRow(new DoubleArrayDataRow(data3));
		ExampleSet newExampleSet = table.createExampleSet();
		allowedAttributes.add(newExampleSet.getAttributes().allAttributes().next());
		Set<Integer> ids = new HashSet<Integer>();
		for (int i=0; i<6; i++) {
			ids.add(i);
		}

		// When 
		Set<ElementaryCondition> elementaryConditions = this.generator.generateElementaryConditions(newExampleSet, allowedAttributes, ids);

		//Then
		// 2 elementary conditions for attribute "d", 18 elementary conditions for attribute "b"
		assertEquals(4, elementaryConditions.size()); 
		assertTrue(elementaryConditions.contains(new ElementaryCondition("num", new Interval(11, Interval.INF, true, false))));
		assertTrue(elementaryConditions.contains(new ElementaryCondition("num", new Interval(Interval.MINUS_INF, 11, false, false))));
		assertTrue(elementaryConditions.contains(new ElementaryCondition("num", new Interval(13.5, Interval.INF, true, false))));
		assertTrue(elementaryConditions.contains(new ElementaryCondition("num", new Interval(Interval.MINUS_INF, 13.5, false, false))));
	}

	@Test
	public void getElementaryConditions_forPartOfSet() {
		// Given
		Set<String> stableAttributes = new HashSet<String>(Arrays.asList("a", "c"));
		this.generator.setStableAttributes(stableAttributes);
		Set<Attribute> allowedAttributes = this.generator.getAttributesAllowedToChange();
		Set<Integer> ids = new HashSet<Integer>();
		for (int i=5; i<10; i++) {
			ids.add(i);
		}

		// When 
		Set<ElementaryCondition> elementaryConditions = this.generator.generateElementaryConditions(exampleSet, allowedAttributes, ids);

		//Then
		// 2 elementary conditions for attribute "d", 18 elementary conditions for attribute "b"
		assertEquals(9, elementaryConditions.size()); 
		assertFalse(elementaryConditions.contains(new ElementaryCondition("b", new Interval(47.5, Interval.INF, true, false))));
		assertFalse(elementaryConditions.contains(new ElementaryCondition("b", new Interval(Interval.MINUS_INF, 47.5, false, false))));
		assertTrue(elementaryConditions.contains(new ElementaryCondition("b", new Interval(7.5, Interval.INF, true, false))));
		assertTrue(elementaryConditions.contains(new ElementaryCondition("b", new Interval(Interval.MINUS_INF, 7.5, false, false))));
		assertTrue(elementaryConditions.contains(new ElementaryCondition("b", new Interval(22.5, Interval.INF, true, false))));
		assertTrue(elementaryConditions.contains(new ElementaryCondition("b", new Interval(Interval.MINUS_INF, 22.5, false, false))));
		assertFalse(elementaryConditions.contains(new ElementaryCondition("d", new SingletonSet(0.0, mappingForAttrD))));
		assertTrue(elementaryConditions.contains(new ElementaryCondition("d", new SingletonSet(1.0, mappingForAttrD))));
	}

	@Test
	public void getBestElementaryCondition_EmptyRule() {
		// Given
		SurvivalRule rule = new SurvivalRule(new CompoundCondition(),
				new ElementaryCondition("survival_status", new SingletonSet(Double.NaN, null)));
		this.generator.setMinRuleCovered(2);

		// When
		ConditionBase wBest = generator.getBestElementaryCondition(rule, null);

		// Then
		assertEquals(new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), wBest);
	}

	@Test
	public void getBestElementaryCondition_NotEmptyRule() {
		// Given
		ElementaryCondition cond = new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false));
		CompoundCondition premise = new CompoundCondition();
		premise.addSubcondition(cond);
		SurvivalRule rule = new SurvivalRule(premise,
				new ElementaryCondition("survival_status", new SingletonSet(Double.NaN, null)));

		this.generator.setMinRuleCovered(2);

		// When
		ConditionBase wBest = generator.getBestElementaryCondition(rule, null);

		// Then
		assertEquals(new ElementaryCondition("a", new Interval(Interval.MINUS_INF, 7.5, false, false)), wBest);
	}

	@Test
	public void getBestElementaryCondition_DifferentUncoveredExampleSet() {
		// Given
		SurvivalRule rule = new SurvivalRule(new CompoundCondition(),
				new ElementaryCondition("survival_status", new SingletonSet(Double.NaN, null)));
		this.generator.setMinRuleCovered(2);

		// Prepare subset with uncovered examples
		int[] elements = new int[exampleSet.size()];
		elements[0] = 1; elements[5] = 1; elements[8] = 1;
		SplittedExampleSet uncoveredExampleSet = new SplittedExampleSet(exampleSet, new Partition(elements, 2));
		uncoveredExampleSet.selectSingleSubset(0);

		// When
		ConditionBase wBest = generator.getBestElementaryCondition(rule, null);

		// Then
		assertEquals(new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), wBest);
	}

	@Test
	public void getBestElementaryCondition_AllAttributesStable() {
		// Given
		this.generator.setStableAttributes(new HashSet<String>(Arrays.asList("a", "b", "c", "d")));
		SurvivalRule rule = new SurvivalRule(new CompoundCondition(),
				new ElementaryCondition("survival_status", new SingletonSet(Double.NaN, null)));
		this.generator.setMinRuleCovered(2);

		// When
		ConditionBase wBest = generator.getBestElementaryCondition(rule, null);

		// Then
		assertEquals(new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), wBest);
	}

	@Test
	public void findCounterCondition_Correct() {
		// Given
		ElementaryCondition condition = new ElementaryCondition("a", new Interval(1.5, Interval.INF, true, false));
		SurvivalRule leftRule = new SurvivalRule(new CompoundCondition(),
				new ElementaryCondition("survival_status", new SingletonSet(Double.NaN, null)));
		leftRule.getPremise().addSubcondition(condition);
		SurvivalRule rightRule = new SurvivalRule(new CompoundCondition(),
				new ElementaryCondition("survival_status", new SingletonSet(Double.NaN, null)));

		// When
		ConditionBase counterCondition = (ElementaryCondition)this.generator.findCounterCondition(condition, leftRule, rightRule);

		// Then
		assertEquals(new ElementaryCondition("a", new Interval(8.5, Interval.INF, true, false)), 
				counterCondition);
	}

	@Test
	public void findCounterCondition_forStableAttributeNotEmptyRules_returnedNull() {
		// Given
		this.generator.setStableAttributes(new HashSet<String>(Arrays.asList("c", "d")));
		ElementaryCondition conditionL1 = new ElementaryCondition("a", new Interval(Interval.MINUS_INF, 7.5, false, false));
		ElementaryCondition conditionR1 = new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false));
		ElementaryCondition conditionL2 = new ElementaryCondition("d", new SingletonSet(1.0, mappingForAttrD));
		SurvivalRule leftRule = new SurvivalRule(new CompoundCondition(),
				new ElementaryCondition("survival_status", new SingletonSet(Double.NaN, null)));
		leftRule.getPremise().addSubcondition(conditionL1);
		leftRule.getPremise().addSubcondition(conditionL2);
		SurvivalRule rightRule = new SurvivalRule(new CompoundCondition(),
				new ElementaryCondition("survival_status", new SingletonSet(Double.NaN, null)));
		rightRule.getPremise().addSubcondition(conditionR1);

		// When
		ConditionBase counterCondition = (ElementaryCondition)this.generator.findCounterCondition(conditionL2, leftRule, rightRule);

		// Then
		assertNull(counterCondition);
	}
	
	@Test
	public void isSurvivalCurveAboveOrEqual_isNotAbove_sameMiddlePoint() {
		// Given
		// Left rule: IF a = <3.5, inf) AND d = {yellow} THEN survival_status = {NaN}
		ElementaryCondition conditionLeft1 = new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false));
		ElementaryCondition conditionLeft2 = new ElementaryCondition("d", new SingletonSet(0.0, mappingForAttrD)); //0.0 = yellow
		SurvivalRule leftRule = new SurvivalRule(new CompoundCondition(),
				new ElementaryCondition("survival_status", new SingletonSet(Double.NaN, null)));
		leftRule.getPremise().addSubcondition(conditionLeft1);
		leftRule.getPremise().addSubcondition(conditionLeft2);

		// Right rule: IF a = (-inf, 9.5) AND d = {green} THEN survival_status = {NaN}
		ElementaryCondition conditionRight1 = new ElementaryCondition("a", new Interval(Interval.MINUS_INF, 9.5, false, false));
		ElementaryCondition conditionRight2 = new ElementaryCondition("d", new SingletonSet(1.0, mappingForAttrD)); //1.0 = green
		SurvivalRule rightRule = new SurvivalRule(new CompoundCondition(),
				new ElementaryCondition("survival_status", new SingletonSet(Double.NaN, null)));
		rightRule.getPremise().addSubcondition(conditionRight1);
		rightRule.getPremise().addSubcondition(conditionRight2);

		Covering leftCovering = leftRule.covers(exampleSet);
		Covering rightCovering = rightRule.covers(exampleSet);

		KaplanMeierEstimator kmLeft = new KaplanMeierEstimator(exampleSet, leftCovering.positives);
		KaplanMeierEstimator kmRight = new KaplanMeierEstimator(exampleSet, rightCovering.positives);

		// When
		boolean isAbove = this.generator.isFulfilledTargetRulePosition(kmRight, kmLeft, TargetRulePosition.BETTER);
		boolean isUnder = this.generator.isFulfilledTargetRulePosition(kmLeft, kmRight, TargetRulePosition.WORSE);

		// Then
		assertFalse(isAbove);
		assertFalse(isUnder);
	}

	@Test
	public void isSurvivalCurveAboveOrEqual_isAbove_differentMiddlePoint() {
		// Given
		// Left rule: IF a = (-inf, 3.5) THEN survival_status = {NaN}
		ElementaryCondition conditionLeft1 = new ElementaryCondition("a", new Interval(Interval.MINUS_INF, 3.5, false, false));
		SurvivalRule leftRule = new SurvivalRule(new CompoundCondition(),
				new ElementaryCondition("survival_status", new SingletonSet(Double.NaN, null)));
		leftRule.getPremise().addSubcondition(conditionLeft1);

		// Right rule: IF a = (-inf, 9.5) AND d = {green} THEN survival_status = {NaN}
		ElementaryCondition conditionRight1 = new ElementaryCondition("a", new Interval(Interval.MINUS_INF, 9.5, false, false));
		ElementaryCondition conditionRight2 = new ElementaryCondition("d", new SingletonSet(1.0, mappingForAttrD)); //1.0 = green
		SurvivalRule rightRule = new SurvivalRule(new CompoundCondition(),
				new ElementaryCondition("survival_status", new SingletonSet(Double.NaN, null)));
		rightRule.getPremise().addSubcondition(conditionRight1);
		rightRule.getPremise().addSubcondition(conditionRight2);

		Covering leftCovering = leftRule.covers(exampleSet);
		Covering rightCovering = rightRule.covers(exampleSet);

		KaplanMeierEstimator kmLeft = new KaplanMeierEstimator(exampleSet, leftCovering.positives);
		KaplanMeierEstimator kmRight = new KaplanMeierEstimator(exampleSet, rightCovering.positives);

		// When
		boolean isAbove = this.generator.isFulfilledTargetRulePosition(kmRight, kmLeft, TargetRulePosition.BETTER);
		boolean isUnder = this.generator.isFulfilledTargetRulePosition(kmLeft, kmRight, TargetRulePosition.WORSE);

		// Then
		assertTrue(isAbove);
		assertTrue(isUnder);
	}

	@Test
	public void calculateAreaUnderKMPlot() {
		// Given
		ElementaryCondition condition = new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false));
		SurvivalRule rule = new SurvivalRule(new CompoundCondition(),
				new ElementaryCondition("survival_status", new SingletonSet(Double.NaN, null)));
		rule.getPremise().addSubcondition(condition);
		Covering covering = rule.covers(exampleSet);
		KaplanMeierEstimator km = new KaplanMeierEstimator(exampleSet, covering.positives);

		// When
		double area = generator.calculateAreaUnderKMPlot(km);

		// Then
		assertEquals(90.0, area, 0.00001);
	}

	@Test
	public void checkStableAttributes_EmptySetStableAttributes_NoError() {
		// Given
		Set<String> stableAttributes = Collections.emptySet();

		// When
		SurvivalActionRulesGenerator.checkStableAttributes(exampleSet, stableAttributes);
	}

	@Test
	public void checkStableAttributes_CorrectSetOfAttributes_NoError() {
		// Given
		Set<String> stableAttributes = new HashSet<String>(Arrays.asList("a", "c"));

		// When
		SurvivalActionRulesGenerator.checkStableAttributes(exampleSet, stableAttributes);
	}

	@Test(expected = IllegalArgumentException.class)
	public void checkStableAttributes_WrongListOfAttributes_Error() {
		// Given
		Set<String> stableAttributes = new HashSet<String>(Arrays.asList("a", "noAttrLikeThis"));

		// When
		SurvivalActionRulesGenerator.checkStableAttributes(exampleSet, stableAttributes);
	}

	@Test
	public void checkExampleSet_correctExampleSet_NoError() {
		SurvivalActionRulesGenerator.checkExampleSet(exampleSet, 2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void checkExampleSet_tooLittleExamples_Error() {
		SurvivalActionRulesGenerator.checkExampleSet(exampleSet, 100);
	}

	@Test(expected = IllegalArgumentException.class)
	public void checkExampleSet_noSurvivalTime_Error() throws OperatorException, OperatorCreationException {
		//Given
		ExampleSet exampleSetNoSurvival = TestUtils.prepareExampleSet();
		Attribute survival_time = exampleSetNoSurvival.getAttributes().get(SurvivalRule.SURVIVAL_TIME_ROLE);
		ExampleTable exTable = exampleSetNoSurvival.getExampleTable();
		exTable.removeAttribute(survival_time);
		exampleSetNoSurvival = exTable.createExampleSet();

		//When
		SurvivalActionRulesGenerator.checkExampleSet(exampleSetNoSurvival, 2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void checkExampleSet_noSurvivalStatus_Error() throws OperatorException, OperatorCreationException {
		//Given
		ExampleSet exampleSetNoSurvival = TestUtils.prepareExampleSet();
		Attribute survival_status = exampleSetNoSurvival.getAttributes().getLabel();
		ExampleTable exTable = exampleSetNoSurvival.getExampleTable();
		exTable.removeAttribute(survival_status);
		exampleSetNoSurvival = exTable.createExampleSet();

		//When
		SurvivalActionRulesGenerator.checkExampleSet(exampleSetNoSurvival, 2);
	}

	@Test
	public void checkUncoveredExampleSet_Correct_NoError() {
		// Given
		int[] elements = new int[exampleSet.size()];
		elements[0] = 1; elements[5] = 1; elements[8] = 1;
		SplittedExampleSet uncoveredExampleSet = new SplittedExampleSet(exampleSet, new Partition(elements, 2));
		uncoveredExampleSet.selectSingleSubset(0);

		//When
		SurvivalActionRulesGenerator.checkUncoveredExampleSet(uncoveredExampleSet, exampleSet, 2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void checkUncoveredExampleSet_UncoveredExampleSetNotInExampleSet_Error() {
		// Given
		int[] elements = new int[exampleSet.size()];
		elements[0] = 1; elements[5] = 1; elements[8] = 1;
		SplittedExampleSet uncoveredExampleSet = new SplittedExampleSet(exampleSet, new Partition(elements, 2));
		SplittedExampleSet disjontExampleSet = new SplittedExampleSet(exampleSet, new Partition(elements, 2));
		disjontExampleSet.selectSingleSubset(0);
		uncoveredExampleSet.selectSingleSubset(1);

		//When
		SurvivalActionRulesGenerator.checkUncoveredExampleSet(uncoveredExampleSet, disjontExampleSet, 2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrongNumberOfMinRuleCovered_sameSizeAsSizeExampleSet_Error() {
		//When
		this.generator.setMinRuleCovered(10);
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrongNumberOfMinRuleCovered_setToZero_Error() {
		//When
		this.generator.setMinRuleCovered(0);
	}
}
