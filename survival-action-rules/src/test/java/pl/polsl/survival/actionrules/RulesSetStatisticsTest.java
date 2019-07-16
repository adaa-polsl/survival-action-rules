package pl.polsl.survival.actionrules;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;

public class RulesSetStatisticsTest {

	@Test
	public void create() throws OperatorException, OperatorCreationException {
		// Given
		ExampleSet exampleSet = TestUtils.prepareExampleSet();
		List<SurvivalActionRule> rules = TestUtils.createDifferentSurvivalRules();

		// When
		RulesSetStatistics statistics = new RulesSetStatistics(rules, exampleSet);

		// Then
		assertEquals(6, statistics.getNumberOfRules());
		assertEquals(1, statistics.getNumberOfRulesWithoutActionInPremise());
		assertEquals(12, statistics.getAggregatedNumberOfConditions());
		assertEquals(2.0, statistics.getMeanNumberConditionsPerRule(), 0.0000001);
		assertEquals(9, statistics.getAggregatedNumberOfActions());
		assertEquals(3, statistics.getAggregatedNumberOfActionsTypeANY());
		assertEquals(1.5, statistics.getMeanNumberOfActionsPerRule(), 0.0000001);
	}
}
