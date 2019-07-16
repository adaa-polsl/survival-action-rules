package pl.polsl.survival.actionrules;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;

public class RuleStatisticTest {
	ExampleSet exampleSet;
	List<SurvivalActionRule> rules;

	@Before
	public void prepareRulesAndExampleSet() throws OperatorException, OperatorCreationException {
		this.exampleSet = TestUtils.prepareExampleSet();
		this.rules = TestUtils.createDifferentSurvivalRules();
	}

	@Test
	public void createStatistic_simpleActionRule() {
		// Given
		SurvivalActionRule rule = this.rules.get(0);

		// When
		RuleStatistic stat = new RuleStatistic(rule, exampleSet);

		// Then
		assertEquals(2, stat.getNumberOfConditions());
		assertEquals(2, stat.getNumberOfActions());
		assertEquals(0, stat.getNumberOfActionTypeANY());
		assertEquals(50, stat.getPercentOfExamplesCoveredByLeftRule(), 1e-10);
		assertEquals(20, stat.getPercentOfExamplesCoveredByRightRule(), 1e-10);
		assertEquals(0.5973115731942719, stat.getPValueLogRankLeftRightRules(), 1e-10);
		assertEquals(90.0, stat.getMedianSurvivalTimeLeftRule(), 1e-10);
		assertEquals(90.0, stat.getMedianSurvivalTimeRightRule(), 1e-10);
		assertEquals(0.0, stat.getDifferenceMedianSurvivalTimeBetweenLeftRight(), 1e-10);
		assertEquals(new HashSet<Integer>(Arrays.asList(5,6,7,8,9)), stat.getCoveredExLeft());
		assertEquals(new HashSet<Integer>(Arrays.asList(8,9)), stat.getCoveredExRight());
	}

	@Test
	public void createStatistic_onlyOneAction() {
		// Given
		SurvivalActionRule rule = this.rules.get(1);

		// When
		RuleStatistic stat = new RuleStatistic(rule, exampleSet);

		// Then
		assertEquals(2, stat.getNumberOfConditions());
		assertEquals(1, stat.getNumberOfActions());
		assertEquals(0, stat.getNumberOfActionTypeANY());
		assertEquals(50, stat.getPercentOfExamplesCoveredByLeftRule(), 1e-10);
		assertEquals(40, stat.getPercentOfExamplesCoveredByRightRule(), 1e-10);
		assertEquals(1.0, stat.getPValueLogRankLeftRightRules(), 1e-10);
		assertEquals(90.0, stat.getMedianSurvivalTimeLeftRule(), 1e-10);
		assertEquals(90.0, stat.getMedianSurvivalTimeRightRule(), 1e-10);
		assertEquals(0.0, stat.getDifferenceMedianSurvivalTimeBetweenLeftRight(), 1e-10);
		assertEquals(new HashSet<Integer>(Arrays.asList(5,6,7,8,9)), stat.getCoveredExLeft());
		assertEquals(new HashSet<Integer>(Arrays.asList(6,7,8,9)), stat.getCoveredExRight());
	}

	@Test
	public void createStatistic_noActions() {
		// Given
		SurvivalActionRule rule = this.rules.get(2);

		// When
		RuleStatistic stat = new RuleStatistic(rule, exampleSet);

		// Then
		assertEquals(2, stat.getNumberOfConditions());
		assertEquals(0, stat.getNumberOfActions());
		assertEquals(0, stat.getNumberOfActionTypeANY());
		assertEquals(40, stat.getPercentOfExamplesCoveredByLeftRule(), 1e-10);
		assertEquals(40, stat.getPercentOfExamplesCoveredByRightRule(), 1e-10);
		assertEquals(1.0, stat.getPValueLogRankLeftRightRules(), 1e-10);
		assertEquals(90.0, stat.getMedianSurvivalTimeLeftRule(), 1e-10);
		assertEquals(90.0, stat.getMedianSurvivalTimeRightRule(), 1e-10);
		assertEquals(0.0, stat.getDifferenceMedianSurvivalTimeBetweenLeftRight(), 1e-10);
		assertEquals(new HashSet<Integer>(Arrays.asList(6,7,8,9)), stat.getCoveredExLeft());
		assertEquals(new HashSet<Integer>(Arrays.asList(6,7,8,9)), stat.getCoveredExRight());
	}

	@Test
	public void createStatistic_actionTypeAny() {
		// Given
		SurvivalActionRule rule = this.rules.get(3);

		// When
		RuleStatistic stat = new RuleStatistic(rule, exampleSet);

		// Then
		assertEquals(2, stat.getNumberOfConditions());
		assertEquals(2, stat.getNumberOfActions());
		assertEquals(1, stat.getNumberOfActionTypeANY());
		assertEquals(80, stat.getPercentOfExamplesCoveredByLeftRule(), 1e-10);
		assertEquals(50, stat.getPercentOfExamplesCoveredByRightRule(), 1e-10);
		assertEquals(0.6961566635522685, stat.getPValueLogRankLeftRightRules(), 1e-10);
		assertEquals(90.0, stat.getMedianSurvivalTimeLeftRule(), 1e-10);
		assertEquals(90.0, stat.getMedianSurvivalTimeRightRule(), 1e-10);
		assertEquals(0.0, stat.getDifferenceMedianSurvivalTimeBetweenLeftRight(), 1e-10);
		assertEquals(new HashSet<Integer>(Arrays.asList(2,3,4,5,6,7,8,9)), stat.getCoveredExLeft());
		assertEquals(new HashSet<Integer>(Arrays.asList(5,6,7,8,9)), stat.getCoveredExRight());
	}

	@Test
	public void createStatistic_2actionsTypeAny() {
		// Given
		SurvivalActionRule rule = this.rules.get(4);

		// When
		RuleStatistic stat = new RuleStatistic(rule, exampleSet);

		// Then
		assertEquals(2, stat.getNumberOfConditions());
		assertEquals(2, stat.getNumberOfActions());
		assertEquals(2, stat.getNumberOfActionTypeANY());
		assertEquals(80, stat.getPercentOfExamplesCoveredByLeftRule(), 1e-10);
		assertEquals(100, stat.getPercentOfExamplesCoveredByRightRule(), 1e-10);
		assertEquals(0.4822956611399176, stat.getPValueLogRankLeftRightRules(), 1e-10);
		assertEquals(90.0, stat.getMedianSurvivalTimeLeftRule(), 1e-10);
		assertEquals(80.0, stat.getMedianSurvivalTimeRightRule(), 1e-10);
		assertEquals(10.0, stat.getDifferenceMedianSurvivalTimeBetweenLeftRight(), 1e-10);
		assertEquals(new HashSet<Integer>(Arrays.asList(2,3,4,5,6,7,8,9)), stat.getCoveredExLeft());
		assertEquals(new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5,6,7,8,9)), stat.getCoveredExRight());
	}

	@Test
	public void createStatistic_twoActionsForThisSameAttribute() {
		// Given
		SurvivalActionRule rule = this.rules.get(5);

		// When
		RuleStatistic stat = new RuleStatistic(rule, exampleSet);

		// Then
		assertEquals(2, stat.getNumberOfConditions());
		assertEquals(2, stat.getNumberOfActions());
		assertEquals(0, stat.getNumberOfActionTypeANY());
		assertEquals(40, stat.getPercentOfExamplesCoveredByLeftRule(), 1e-10);
		assertEquals(30, stat.getPercentOfExamplesCoveredByRightRule(), 1e-10);
		assertEquals(0.18227863666936783, stat.getPValueLogRankLeftRightRules(), 1e-10);
		assertEquals(70.0, stat.getMedianSurvivalTimeLeftRule(), 1e-10);
		assertEquals(90.0, stat.getMedianSurvivalTimeRightRule(), 1e-10);
		assertEquals(-20.0, stat.getDifferenceMedianSurvivalTimeBetweenLeftRight(), 1e-10);
		assertEquals(new HashSet<Integer>(Arrays.asList(3,4,5,6)), stat.getCoveredExLeft());
		assertEquals(new HashSet<Integer>(Arrays.asList(7,8,9)), stat.getCoveredExRight());
	}
}
