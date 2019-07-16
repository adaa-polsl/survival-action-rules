package pl.polsl.survival.actionrules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;

import adaa.analytics.rules.logic.representation.Action;
import adaa.analytics.rules.logic.representation.CompoundCondition;
import adaa.analytics.rules.logic.representation.ElementaryCondition;
import adaa.analytics.rules.logic.representation.Interval;
import adaa.analytics.rules.logic.representation.Logger;
import pl.polsl.survival.actionrules.ExperimentExecutor.DatasetWrapper;
import pl.polsl.survival.actionrules.ExperimentExecutor.ParamSetWrapper;

public class ExperimentExecutorTest {
	private ExperimentExecutor executor;
	private static List<String> mappingForAttrD;

	@Before
	public void createExperimentExecutor() throws OperatorException, OperatorCreationException {
		this.executor = new ExperimentExecutor();
		mappingForAttrD = TestUtils.prepareExampleSet().getAttributes().get("d").getMapping().getValues();
	}

	@Test
	public void execute() throws ParserConfigurationException, SAXException, IOException, OperatorException, OperatorCreationException {
		// Given
		String config = TestUtils.class.getResource(TestUtils.CONFIG_FILE).getPath();
		List<SurvivalActionRule> expectedRules = new ArrayList<SurvivalActionRule>();
		Action action11 = new Action(
				new ElementaryCondition("b", new Interval(37.5, Interval.INF, true, false)),
				new ElementaryCondition("b", new Interval(17.5, Interval.INF, true, false)));
		CompoundCondition premise1 = new CompoundCondition();
		premise1.addSubcondition(action11);
		SurvivalActionRule actionRule1 = new SurvivalActionRule(premise1, SurvivalActionRule.createConsequence("status"));
		Action action21 = new Action(
				new ElementaryCondition("b", new Interval(12.5, Interval.INF, true, false)),
				new ElementaryCondition("b", new Interval(17.5, 37.5, true, false)));
		CompoundCondition premise2 = new CompoundCondition();
		premise2.addSubcondition(action21);
		SurvivalActionRule actionRule2 = new SurvivalActionRule(premise2, SurvivalActionRule.createConsequence("status"));
		expectedRules.add(actionRule1);
		expectedRules.add(actionRule2);

		// When
		List<SurvivalActionRule> generatedRules = this.executor.execute(config);

		// Then
		assertEquals(expectedRules, generatedRules);
	}

	private List<ParamSetWrapper> readParams() throws ParserConfigurationException, SAXException, IOException {
		Document doc = getConfigDocument();
		return this.executor.getParameterSets(doc);
	}

	private List<DatasetWrapper> readDatasets() throws ParserConfigurationException, SAXException, IOException {
		Document doc = getConfigDocument();
		return this.executor.getDatasets(doc);
	}

	private Document getConfigDocument() throws ParserConfigurationException, SAXException, IOException {
		String config = TestUtils.class.getResource(TestUtils.CONFIG_FILE).getPath();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		return dBuilder.parse(config);
	}

	@Test
	public void writeResults() throws ParserConfigurationException, SAXException, IOException, OperatorException, OperatorCreationException {
		// Given
		String config = TestUtils.class.getResource(TestUtils.CONFIG_FILE).getPath();
		File configFile = new File(config);
		String outputDir = configFile.getParentFile().getAbsolutePath().toString() + File.separator;
		List<SurvivalActionRule> rules = TestUtils.createDifferentSurvivalRules();
		DatasetWrapper datasetWrapper = readDatasets().get(0);
		ExampleSet exampleSet = TestUtils.prepareExampleSet();

		// When
		this.executor.writeResults(rules, exampleSet, datasetWrapper.datasetDirectory.get(0), outputDir, 5, 
				TargetRulePosition.BETTER, datasetWrapper.stableAttributes);
	}

	@Test
	public void getParameterSets() throws ParserConfigurationException, SAXException, IOException {
		// Given
		Document doc = TestUtils.readConfFile();
		Logger.getInstance().addStream(System.out, Level.FINE);
		List<ParamSetWrapper> paramSets = new ArrayList<>();
		ParamSetWrapper wrapper = new ParamSetWrapper();
		wrapper.name = "auto";
		wrapper.map.put("min_rule_covered", "3");
		wrapper.map.put("target_survival_curve", "better");
		paramSets.add(wrapper);

		// When
		List<ParamSetWrapper> actualParamSets = this.executor.getParameterSets(doc);

		// Then
		assertTrue(paramSets.containsAll(actualParamSets));
	}

	@Test
	public void getDatasets() throws ParserConfigurationException, SAXException, IOException {
		// Given
		Document doc = TestUtils.readConfFile();
		Logger.getInstance().addStream(System.out, Level.FINE);
		List<String> datasetsDirs = new ArrayList<String>();
		datasetsDirs.add("test_survival_dataset_v2.arff");
		Set<String> stableAttr = new HashSet<String>();
		stableAttr.add("a");
		stableAttr.add("c");

		// When
		List<DatasetWrapper> datasets = this.executor.getDatasets(doc);

		// Then
		assertEquals(2, datasets.size());
		assertEquals(datasetsDirs, datasets.get(0).datasetDirectory);
		assertEquals("status", datasets.get(0).label);
		assertEquals("time", datasets.get(0).survivalTime);
		assertEquals("status", datasets.get(1).label);
		assertEquals("time", datasets.get(1).survivalTime);
		assertEquals("./test-output1/", datasets.get(0).outputDirectory);
		assertEquals("./test-output2/", datasets.get(1).outputDirectory);
		assertEquals(stableAttr, datasets.get(1).stableAttributes);
	}
}
