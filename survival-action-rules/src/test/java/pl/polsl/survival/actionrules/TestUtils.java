package pl.polsl.survival.actionrules;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer5.operator.io.ArffExampleSource;

import adaa.analytics.rules.logic.representation.Action;
import adaa.analytics.rules.logic.representation.AnyValueSet;
import adaa.analytics.rules.logic.representation.CompoundCondition;
import adaa.analytics.rules.logic.representation.ElementaryCondition;
import adaa.analytics.rules.logic.representation.Interval;
import adaa.analytics.rules.logic.representation.SingletonSet;
import adaa.analytics.rules.logic.representation.SurvivalRule;
import adaa.analytics.rules.utils.RapidMiner5;

public class TestUtils {

	final static String TEST_DATASET = "test_survival_dataset.arff";
	final static String CONFIG_FILE = "test_survival_dataset_v2.xml";

	public static ExampleSet prepareExampleSet() throws OperatorException, OperatorCreationException {
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.COMMAND_LINE);
		RapidMiner.init();

		ArffExampleSource arffSource = RapidMiner5.createOperator(ArffExampleSource.class);

		arffSource.setParameter(ArffExampleSource.PARAMETER_DATA_FILE, TestUtils.class.getResource(TEST_DATASET).getPath());

		com.rapidminer.Process process = new com.rapidminer.Process();
		process.getRootOperator().getSubprocess(0).addOperator(arffSource);
		arffSource.getOutputPorts().getPortByName("output")
		.connectTo(process.getRootOperator().getSubprocess(0).getInnerSinks().getPortByIndex(0));
		IOContainer out = process.run();
		ExampleSet exampleSet = out.get(ExampleSet.class, 0);

		// Set attributes
		Attribute survivalStatusAttr = exampleSet.getAttributes().get("survival_status");
		exampleSet.getAttributes().setLabel(survivalStatusAttr);		
		Attribute survivalTimeAttr = exampleSet.getAttributes().get(SurvivalRule.SURVIVAL_TIME_ROLE);
		exampleSet.getAttributes().setSpecialAttribute(survivalTimeAttr, SurvivalRule.SURVIVAL_TIME_ROLE);

		return exampleSet;
	}

	public static SurvivalActionRule prepareSurvivalActionRule() {
		Action consequence = new Action(
				getLabel(),
				new SingletonSet(Double.NaN, null),
				new SingletonSet(Double.NaN, null));

		Action actionA = new Action(
				"a",
				new Interval(0.0, 4.0, true, true),
				new Interval(6.0, 10.0, true, true));
		Action actionB = new Action(
				"b",
				new Interval(40.0, 50.0, true, true),
				new Interval(0.0, 25.0, true, true));

		CompoundCondition premise = new CompoundCondition();
		premise.addSubcondition(actionA);
		premise.addSubcondition(actionB);

		return new SurvivalActionRule(premise, consequence);
	}

	public static String getLabel() {
		return "survival_status";
	}

	public static Document readConfFile() throws ParserConfigurationException, SAXException, IOException {
		String config = TestUtils.class.getResource(CONFIG_FILE).getPath();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(config);
		return doc;
	}

	public static List<SurvivalActionRule> createDifferentSurvivalRules() {
		List<SurvivalActionRule> rules = new ArrayList<SurvivalActionRule>();

		// First rule - normal action rule, only actions
		Action action11 = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)),
				new ElementaryCondition("a", new Interval(8.5, Interval.INF, true, false)));
		Action action12 = new Action(
				new ElementaryCondition("c", new Interval(6.5, Interval.INF, true, false)),
				new ElementaryCondition("c", new Interval(7.5, Interval.INF, true, false)));
		CompoundCondition premise1 = new CompoundCondition();
		premise1.addSubcondition(action11);
		premise1.addSubcondition(action12);
		SurvivalActionRule actionRule1 = new SurvivalActionRule(premise1, SurvivalActionRule.createConsequence("survival_status"));
		rules.add(actionRule1);

		// Second rule - only one action
		Action action21 = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)),
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)));
		Action action22 = new Action(
				new ElementaryCondition("c", new Interval(6.5, Interval.INF, true, false)),
				new ElementaryCondition("c", new Interval(7.5, Interval.INF, true, false)));
		CompoundCondition premise2 = new CompoundCondition();
		premise2.addSubcondition(action21);
		premise2.addSubcondition(action22);
		SurvivalActionRule actionRule2 = new SurvivalActionRule(premise2, SurvivalActionRule.createConsequence("survival_status"));
		rules.add(actionRule2);

		// Third rule - no actions
		Action action31 = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)),
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)));
		Action action32 = new Action(
				new ElementaryCondition("c", new Interval(7.5, Interval.INF, true, false)),
				new ElementaryCondition("c", new Interval(7.5, Interval.INF, true, false)));
		CompoundCondition premise3 = new CompoundCondition();
		premise3.addSubcondition(action31);
		premise3.addSubcondition(action32);
		SurvivalActionRule actionRule3 = new SurvivalActionRule(premise3, SurvivalActionRule.createConsequence("survival_status"));
		rules.add(actionRule3);

		// Fourth rule - action type ANY
		Action action41 = new Action(
				new ElementaryCondition("a", new Interval(1.5, Interval.INF, true, false)),
				new ElementaryCondition("a", new Interval(5.5, Interval.INF, true, false)));
		Action action42 = new Action(
				new ElementaryCondition("c", new Interval(3.5, Interval.INF, true, false)),
				new ElementaryCondition("c", new AnyValueSet()));
		CompoundCondition premise4 = new CompoundCondition();
		premise4.addSubcondition(action41);
		premise4.addSubcondition(action42);
		SurvivalActionRule actionRule4 = new SurvivalActionRule(premise4, SurvivalActionRule.createConsequence("survival_status"));
		rules.add(actionRule4);

		// Fifth rule - 2 actions type ANY
		Action action51 = new Action(
				new ElementaryCondition("a", new Interval(2.5, Interval.INF, true, false)),
				new ElementaryCondition("a", new AnyValueSet()));
		Action action52 = new Action(
				new ElementaryCondition("c", new Interval(3.5, Interval.INF, true, false)),
				new ElementaryCondition("c", new AnyValueSet()));
		CompoundCondition premise5 = new CompoundCondition();
		premise5.addSubcondition(action51);
		premise5.addSubcondition(action52);
		SurvivalActionRule actionRule5 = new SurvivalActionRule(premise5, SurvivalActionRule.createConsequence("survival_status"));
		rules.add(actionRule5);

		// Sixth rule - two actions for this same attribute
		Action action61 = new Action(
				new ElementaryCondition("a", new Interval(2.5, Interval.INF, true, false)),
				new ElementaryCondition("a", new Interval(6.5, Interval.INF, true, false)));
		Action action62 = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)),
				new ElementaryCondition("a", new Interval(7.5, Interval.INF, true, false)));
		Action action63 = new Action(
				new ElementaryCondition("c", new Interval(Interval.MINUS_INF, 8.5, false, false)),
				new ElementaryCondition("c", new Interval(6.5, Interval.INF, true, false)));
		CompoundCondition premise6 = new CompoundCondition();
		premise6.addSubcondition(action61);
		premise6.addSubcondition(action62);
		premise6.addSubcondition(action63);
		SurvivalActionRule actionRule6 = new SurvivalActionRule(premise6, SurvivalActionRule.createConsequence("survival_status"));
		rules.add(actionRule6);

		return rules;
	}
}
