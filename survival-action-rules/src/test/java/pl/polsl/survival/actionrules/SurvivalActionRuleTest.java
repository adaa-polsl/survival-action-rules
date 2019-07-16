package pl.polsl.survival.actionrules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;

import adaa.analytics.rules.logic.induction.ActionCovering;
import adaa.analytics.rules.logic.representation.Action;
import adaa.analytics.rules.logic.representation.AnyValueSet;
import adaa.analytics.rules.logic.representation.CompoundCondition;
import adaa.analytics.rules.logic.representation.ElementaryCondition;
import adaa.analytics.rules.logic.representation.Interval;
import adaa.analytics.rules.logic.representation.SingletonSet;
import adaa.analytics.rules.logic.representation.SurvivalRule;

public class SurvivalActionRuleTest {
	private static ExampleSet exampleSet;

	@BeforeClass
	public static void init() throws OperatorException, OperatorCreationException {
		exampleSet = TestUtils.prepareExampleSet();
	}

	@Test
	public void create() {
		// Given
		Action consequence = new Action(
				TestUtils.getLabel(),
				new SingletonSet(Double.NaN, null),
				new SingletonSet(Double.NaN, null));

		Action actionA = new Action(
				"a",
				new Interval(0.0, 4.0, true, true),
				new Interval(6.0, 10.0, true, true));
		CompoundCondition premise = new CompoundCondition();
		premise.addSubcondition(actionA);
		
		// When
		SurvivalActionRule rule = new SurvivalActionRule(premise, consequence);
		
		//Then
		assertEquals(premise, rule.getPremise());
		assertEquals(consequence, rule.getConsequence());
	}
	
	@Test
	public void createEmpty() {
		// Given
		
		// When
		SurvivalActionRule rule = new SurvivalActionRule(null);
		
		// Then
		assertNotNull(rule.getConsequence());
		assertEquals(new CompoundCondition(), rule.getPremise());
	}
	
	@Test
	public void getLeftRule() {
		// Given
		SurvivalActionRule survivalActionRule = TestUtils.prepareSurvivalActionRule();
		
		CompoundCondition normalSurvivalRulePremise = new CompoundCondition();
		normalSurvivalRulePremise.addSubcondition(new ElementaryCondition("a", new Interval(0.0, 4.0, true, true)));
		normalSurvivalRulePremise.addSubcondition(new ElementaryCondition("b", new Interval(40.0, 50.0, true, true)));		
		ElementaryCondition normalSurvivalRuleConsequence = new ElementaryCondition(TestUtils.getLabel(), new SingletonSet(Double.NaN, null));
		SurvivalRule survivalRule = new SurvivalRule(normalSurvivalRulePremise, normalSurvivalRuleConsequence);
		
		// When
		SurvivalRule leftActionRule = (SurvivalRule)survivalActionRule.getLeftRule();
		
		// Then
		assertEquals(survivalRule.getPremise(), leftActionRule.getPremise());
		assertEquals(survivalRule.getConsequence(), leftActionRule.getConsequence());		
	}
	
	@Test
	public void getRightRule() {
		// Given
		SurvivalActionRule survivalActionRule = TestUtils.prepareSurvivalActionRule();
		
		CompoundCondition normalSurvivalRulePremise = new CompoundCondition();
		normalSurvivalRulePremise.addSubcondition(new ElementaryCondition("a", new Interval(6.0, 10.0, true, true)));
		normalSurvivalRulePremise.addSubcondition(new ElementaryCondition("b", new Interval(0.0, 25.0, true, true)));		
		ElementaryCondition normalSurvivalRuleConsequence = new ElementaryCondition(TestUtils.getLabel(), new SingletonSet(Double.NaN, null));
		SurvivalRule survivalRule = new SurvivalRule(normalSurvivalRulePremise, normalSurvivalRuleConsequence);
		
		// When
		SurvivalRule rightActionRule = (SurvivalRule)survivalActionRule.getRightRule();
		
		// Then
		assertEquals(survivalRule.getPremise(), rightActionRule.getPremise());
		assertEquals(survivalRule.getConsequence(), rightActionRule.getConsequence());		
	}
	
	@Test
	public void cover() {
		// Given
		SurvivalActionRule rule = TestUtils.prepareSurvivalActionRule();
		
		// When
		ActionCovering covering = (ActionCovering)rule.covers(exampleSet);
		
		// Then
		// Left part of rule covers 3 examples. Right part of rule covers 5 examples.
		assertEquals(3.0, covering.weighted_p, 1e-10);
		assertEquals(10.0, covering.weighted_P, 1e-10);
		assertEquals(5.0, covering.weighted_pRight, 1e-10);
		assertEquals(10.0, covering.weighted_P_right, 1e-10);
		assertEquals(0.0, covering.weighted_n, 1e-10);
		assertEquals(0.0, covering.weighted_N, 1e-10);
		assertEquals(0.0, covering.weighted_nRight, 1e-10);
		assertEquals(0.0, covering.weighted_N_right, 1e-10);
	}

	@Test
	public void getLogRankValueLeftRightRule() throws OperatorException, OperatorCreationException {
		// Given
		Action action = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(8.5, Interval.INF, true, false)));
		CompoundCondition premise = new CompoundCondition();
		premise.addSubcondition(action);
		SurvivalActionRule rule = new SurvivalActionRule(premise, SurvivalActionRule.createConsequence("survival_status"));
		ExampleSet exampleSet = TestUtils.prepareExampleSet();

		// When
		double logRankValue = rule.getLogRankValueLeftRightRule(exampleSet);

		// Then
		assertEquals(0.2790697674418606, logRankValue, 0.00001);
	}
	
	@Test
	public void reduceActionsThisSameAttribute() {
		// Given
		Action action1 = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(8.5, Interval.INF, true, false)));
		Action action2 = new Action(
				new ElementaryCondition("a", new Interval(4.5, Interval.INF, false, false)), 
				new ElementaryCondition("a", new Interval(6.5, Interval.INF, true, false)));
		CompoundCondition inputPremise = new CompoundCondition();
		inputPremise.addSubcondition(action1);
		inputPremise.addSubcondition(action2);
		SurvivalActionRule actionRule = new SurvivalActionRule(inputPremise, SurvivalActionRule.createConsequence("survival_status"));
		
		Action actionExpected = new Action(
				new ElementaryCondition("a", new Interval(4.5, Interval.INF, false, false)), 
				new ElementaryCondition("a", new Interval(8.5, Interval.INF, true, false)));
		CompoundCondition expectedPremise = new CompoundCondition();
		expectedPremise.addSubcondition(actionExpected);
		SurvivalActionRule actionRuleExpected = new SurvivalActionRule(expectedPremise, SurvivalActionRule.createConsequence("survival_status"));
		
		// When
		SurvivalActionRule shortened = actionRule.reduceActionsThisSameAttribute();
		
		// Then
		assertEquals(actionRuleExpected, shortened);
	}
	
	@Test
	public void reduceActionsThisSameAttribute_withEmptyAction() {
		// Given
		Action action1 = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(8.5, Interval.INF, true, false)));
		Action action2 = new Action(
				new ElementaryCondition("a", new Interval(4.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new AnyValueSet()));
		CompoundCondition firstPremise = new CompoundCondition();
		firstPremise.addSubcondition(action1);
		firstPremise.addSubcondition(action2);
		SurvivalActionRule actionRuleFirst = new SurvivalActionRule(firstPremise, SurvivalActionRule.createConsequence("survival_status"));
		
		CompoundCondition secondPremise = new CompoundCondition();
		secondPremise.addSubcondition(action2);
		secondPremise.addSubcondition(action1);
		SurvivalActionRule actionRuleSecond = new SurvivalActionRule(firstPremise, SurvivalActionRule.createConsequence("survival_status"));
		
//		Action actionExpected = new Action(
//				new ElementaryCondition("a", new Interval(4.5, Interval.INF, true, false)), 
//				new ElementaryCondition("a", new Interval(8.5, Interval.INF, true, false)));
//		CompoundCondition expectedPremise = new CompoundCondition();
//		expectedPremise.addSubcondition(actionExpected);
//		SurvivalActionRule actionRuleExpected = new SurvivalActionRule(expectedPremise, SurvivalActionRule.createConsequence("survival_status"));
		
		// When
		SurvivalActionRule shortenedFirst = actionRuleFirst.reduceActionsThisSameAttribute();
		SurvivalActionRule shortenedSecond = actionRuleSecond.reduceActionsThisSameAttribute();
		
		System.out.println(shortenedFirst);
		System.out.println(shortenedSecond);
		// Then
		assertTrue(shortenedFirst.equals(shortenedSecond));
	}
}
