package pl.polsl.survival.actionrules;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;
import org.h2.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer5.operator.io.ArffExampleSource;

import adaa.analytics.rules.logic.induction.Covering;
import adaa.analytics.rules.logic.representation.KaplanMeierEstimator;
import adaa.analytics.rules.logic.representation.Logger;
import adaa.analytics.rules.logic.representation.SurvivalRule;
import adaa.analytics.rules.utils.RapidMiner5;

public class ExperimentExecutor {

	private final static String PARAM_MIN_RULE_COVERED = "min_rule_covered";
	private final static String PARAM_TARGET_RULE_POSITION = "target_survival_curve";
	private final static String PARAM_MAX_COMMON_EXAMPLES_PERCENTAGE = "max_common_examples_percentage";
	private final static String PARAM_MAX_RULE_COVERING_PERCENTAGE = "max_rule_covering_percentage";

	protected static class ParamSetWrapper {
		String name;
		final Map<String, Object> map = new TreeMap<>();
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null) return false;
			if (getClass() != o.getClass()) return false;
			ParamSetWrapper wrapper = (ParamSetWrapper) o;
			return this.name.equals(wrapper.name) &&
					this.map.equals(wrapper.map);
		}
	}

	protected static class DatasetWrapper {
		String label;
		String survivalTime;
		Set<String> stableAttributes;
		String outputDirectory;
		List<String> datasetDirectory;
	}

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, OperatorException, OperatorCreationException {
		if (args.length == 1) {
			ExperimentExecutor executor = new ExperimentExecutor();
			executor.execute(args[0]);
		} else {
			throw new IllegalArgumentException("Please specify path to configuration file.");
		}
	}

	public List<SurvivalActionRule> execute(String configFilePath) throws ParserConfigurationException, SAXException, IOException, OperatorException, OperatorCreationException {
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.COMMAND_LINE);
		RapidMiner.init();

		File configFile = new File(configFilePath);
		String configDir = configFile.getAbsoluteFile().getParentFile().getAbsolutePath() + File.separator;

		Logger.getInstance().addStream(System.out, Level.FINE);

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(configFilePath);

		String lineSeparator = System.getProperty("line.separator");
		Logger.log("Configuration file loaded." + lineSeparator, Level.INFO);

		List<ParamSetWrapper> params = getParameterSets(doc);
		List<DatasetWrapper> datasets = getDatasets(doc);

		List<SurvivalActionRule> rules = null;

		TargetRulePosition induceBetterSurvival;
		int minRuleCovered;
		float maxCommonExamplesPercentage;
		float maxRuleCoveringPercentage;
		for (int i=0; i < params.size(); i++) {
			induceBetterSurvival = SurvivalActionRulesGenerator.DEFAULT_INDUCE_BETTER_SURVIVAL;
			minRuleCovered = SurvivalActionRulesGenerator.DEFAULT_MIN_COVERED;
			maxCommonExamplesPercentage = SurvivalActionRulesGenerator.DEFAULT_MAX_COMMON_EXAMPLES_PER;
			maxRuleCoveringPercentage = SurvivalActionRulesGenerator.DEFAULT_MAX_RULE_COVERING_PERCENTAGE;
			ParamSetWrapper parameterSet = params.get(i);
			if (parameterSet.map.containsKey(PARAM_MIN_RULE_COVERED)) {
				minRuleCovered = Integer.parseInt((String)parameterSet.map.get(PARAM_MIN_RULE_COVERED));
			}
			if (parameterSet.map.containsKey(PARAM_TARGET_RULE_POSITION)) {
				induceBetterSurvival = readParamTargetSurvivalCurve(parameterSet);
			}
			if (parameterSet.map.containsKey(PARAM_MAX_COMMON_EXAMPLES_PERCENTAGE)) {
				maxCommonExamplesPercentage = Float.parseFloat((String)parameterSet.map.get(PARAM_MAX_COMMON_EXAMPLES_PERCENTAGE));
			}
			if (parameterSet.map.containsKey(PARAM_MAX_RULE_COVERING_PERCENTAGE)) {
				maxRuleCoveringPercentage = Float.parseFloat((String)parameterSet.map.get(PARAM_MAX_RULE_COVERING_PERCENTAGE));
			}

			for (int j = 0; j < datasets.size(); j++) {
				DatasetWrapper dataset = datasets.get(j);
				if (dataset.label == null) {
					dataset.label = "survival_status";
				}
				if (dataset.survivalTime == null) {
					dataset.survivalTime = "survival_time";
				}
				if (dataset.stableAttributes == null) {
					dataset.stableAttributes = SurvivalActionRulesGenerator.EMPTY_STABLE_ATTRIBUTES;
				}
				for (String arffFile: dataset.datasetDirectory) {
					System.out.println(arffFile);
					ExampleSet exampleSet = getExampleSet(configDir + arffFile, dataset.label, dataset.survivalTime);
					ExampleSet uncoveredExampleSet = (ExampleSet)exampleSet.clone();
					SurvivalActionRulesGenerator generator = SurvivalActionRulesGenerator.createSurvivalActionRulesGenerator(
							exampleSet, uncoveredExampleSet, minRuleCovered, induceBetterSurvival, maxCommonExamplesPercentage, maxRuleCoveringPercentage, dataset.stableAttributes);
					rules = generator.induceSurvivalActionRuleList(); // genearte rules
					Logger.log("Generated rules: " + rules + lineSeparator, Level.INFO);
					writeResults(new ArrayList<SurvivalActionRule>(rules), exampleSet,
							arffFile, createOutputDirName(configDir, dataset.outputDirectory, parameterSet.name),
							minRuleCovered, induceBetterSurvival, dataset.stableAttributes);
					Logger.log("Results saved in directory " + configDir + dataset.outputDirectory + lineSeparator, Level.INFO);
				}
			}
		}
		//RapidMiner.quit(RapidMiner.ExitMode.NORMAL);
		return rules;
	}

	private String createOutputDirName(String configDir, String outputDir, String parameterSetName) {
		if (!configDir.substring(configDir.length() - 1).equals(File.separator)) {
			configDir = configDir + File.separator;
		}
		if (!outputDir.substring(outputDir.length() - 1).equals(File.separator)) {
			outputDir = outputDir + File.separator;
		}
		return configDir + outputDir + parameterSetName + File.separator;
	}

	private TargetRulePosition readParamTargetSurvivalCurve(ParamSetWrapper parameterSet) {
		String param = (String)parameterSet.map.get(PARAM_TARGET_RULE_POSITION);
		param.toLowerCase();
		if (param.equals("any") || param.equals("a")) {
			return TargetRulePosition.ANY;
		}
		if (param.equals("better") || param.equals("b")) {
			return TargetRulePosition.BETTER;
		}
		if (param.equals("worse") || param.equals("w")) {
			return TargetRulePosition.WORSE;
		}
		throw new IllegalArgumentException("Parameter " + PARAM_MIN_RULE_COVERED +
				" has wrong value. It should be 'better', 'worse' or 'any'.");
	}

	public void writeResults(List<SurvivalActionRule> rulesSet, ExampleSet exampleSet, String exampleSetName,
			String outputDir, int minRuleCovered, TargetRulePosition targetRulePosition,
			Set<String> stableAttributes) throws FileNotFoundException {

		File directory = new File(outputDir);
		if (! directory.exists()){
			directory.mkdirs();
		}

		String fileNameStatisticRuleSet = generateNameOfFile("result", "txt", outputDir, exampleSetName);
		String fileNameEstimator = generateNameOfFile("estimator", "csv", outputDir, exampleSetName);
		String fileNameStatEachRule = generateNameOfFile("eachRuleStatistic", "csv", outputDir, exampleSetName);
		String fileNameCovering = generateNameOfFile("coveredExamples", "txt", outputDir, exampleSetName);

		RulesSetStatistics ruleSetStatistic = new RulesSetStatistics(rulesSet, exampleSet);

		writeRuleSetResults(fileNameStatisticRuleSet, ruleSetStatistic, exampleSetName, minRuleCovered, targetRulePosition, stableAttributes);
		writeEstimator(fileNameEstimator, rulesSet, exampleSet);
		writeRulesStatistics(fileNameStatEachRule, ruleSetStatistic);
		writeCovering(fileNameCovering, ruleSetStatistic);
	}

	private void writeCovering(String fileName, RulesSetStatistics statistics) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(new FileOutputStream(fileName, false));
		for (int i = 0; i < statistics.getRuleStatistics().size(); i++) {
			RuleStatistic ruleStatistic = statistics.getRuleStatistics().get(i);
			writer.print("r" + (i+1) + "_l: ");
			writer.println(ruleStatistic.getCoveredExLeft());
			writer.print("r" + (i+1) + "_r: ");
			writer.println(ruleStatistic.getCoveredExRight());
		}
		writer.close();
	}

	private void writeRulesStatistics(String fileName, RulesSetStatistics statistics) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(new FileOutputStream(fileName, false));
		writer.println("rule, nb_conditions, nb_actions, nb_actions_ANY, percent_examples_covered_left_rule, " +
				"percent_examples_covered_right_rule, pValue_logRank, median_survival_time_left_rule, " +
				"median_survival_time_right_rule, diff_median_survival_time");
		List<RuleStatistic> ruleStatList = statistics.getRuleStatistics();
		for (int i = 0; i < ruleStatList.size(); i++) {
			RuleStatistic stat = ruleStatList.get(i);
			StringJoiner joiner = new StringJoiner(", ");
			joiner
			.add("r" + (i + 1))
			.add(Integer.toString(stat.getNumberOfConditions()))
			.add(Integer.toString(stat.getNumberOfActions()))
			.add(Integer.toString(stat.getNumberOfActionTypeANY()))
			.add(Double.toString(stat.getPercentOfExamplesCoveredByLeftRule()))
			.add(Double.toString(stat.getPercentOfExamplesCoveredByRightRule()))
			.add(Double.toString(stat.getPValueLogRankLeftRightRules()))
			.add(Double.toString(stat.getMedianSurvivalTimeLeftRule()))
			.add(Double.toString(stat.getMedianSurvivalTimeRightRule()))
			.add(Double.toString(stat.getPercentOfExamplesCoveredByLeftAndRightRules()))
			.add(Double.toString(stat.getDifferenceMedianSurvivalTimeBetweenLeftRight()));
			String statTextCombine = joiner.toString();
			writer.println(statTextCombine);

		}
		writer.close();
	}

	private void writeEstimator(String fileName, List<SurvivalActionRule> rules, ExampleSet exampleSet) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(new FileOutputStream(fileName, false));
		StringBuilder sb = new StringBuilder();
		KaplanMeierEstimator km = new KaplanMeierEstimator(exampleSet);
		// get times from training estimator
		List<SurvivalRule> survivalRules = new ArrayList<SurvivalRule>();
		for (SurvivalActionRule actionRule: rules) {
			survivalRules.add(actionRule.getLeftRule());
			survivalRules.add(actionRule.getRightRule());
		}
		ArrayList<Double> times = km.getTimes();
		// build header
		sb.append("time, entire-set, ");
		for (int i = 0; i < rules.size(); ++i) {
			for(int j = 0; j <= 1; j++) {
				String side = (j==0) ? "l" : "r";
				sb.append("r" + (i + 1) + "_" + side + ", ");
			}
		}
		sb.append("\n");
		// write probability
		for (double t : times) {
			sb.append(t + ", " + km.getProbabilityAt(t) + ", ");
			for (SurvivalRule r: survivalRules) {
				Covering cov = r.covers(exampleSet);
				KaplanMeierEstimator kme = new KaplanMeierEstimator(exampleSet, cov.positives);
				sb.append(kme.getProbabilityAt(t) + ", ");
			}
			sb.append("\n");
		}
		String estimator = sb.toString();
		writer.println(estimator);
		writer.close();
	}

	private void writeRuleSetResults(String fileName, RulesSetStatistics statistic, String exampleSetName,
			int minRuleCovered, TargetRulePosition targetRulePosition, Set<String> stableAttributes) throws FileNotFoundException {

		PrintWriter writer = new PrintWriter(new FileOutputStream(fileName, false));
		writer.println("Dataset: " + exampleSetName);
		writer.println("Number of examples: " + statistic.getExampleSet().size());
		writer.println("Minimum number of covered examples by rule: " + minRuleCovered);
		writer.println("Stable attributes: " + stableAttributes);
		writer.println("Position of a target rule: " + targetRulePosition.toString());
		writer.println("Number of rules: " + statistic.getNumberOfRules());
		writer.println("Number of rules without any action in premise: " + statistic.getNumberOfRulesWithoutActionInPremise());
		writer.println("Conditions count: " + statistic.getAggregatedNumberOfConditions());
		writer.println("Actions count: " + statistic.getAggregatedNumberOfActions());
		writer.println("\"Any\" actions count: " + statistic.getAggregatedNumberOfActionsTypeANY());
		writer.println("Averege conditions per rule: " + statistic.getMeanNumberConditionsPerRule());
		writer.println("Averege actions per rule: " + statistic.getMeanNumberOfActionsPerRule());
		writer.println("Mean percent of examples covered by left and right rules: " + statistic.getMeanPercentOfExamplesCoveredByLeftAndRightRules());
		writer.println("Mean percent of examples covered by left rule: " + statistic.getMeanPercentOfExamplesCoveredByLeftRule());
		writer.println("Mean percent of examples covered by right rule: " + statistic.getMeanPercentOfExamplesCoveredByRightRule());
		writer.println("Percent of examples covered by left and right rules: " + statistic.getPercentOfExamplesCoveredByLeftAndRightRules());
		writer.println("Percent of examples covered by left or right rules: " + statistic.getPercentOfExamplesCoveredByLeftOrRightRules());
		writer.println("Percent of examples covered by left rule: " + statistic.getPercentOfExamplesCoveredByLeftRule());
		writer.println("Percent of examples covered by right rule: " + statistic.getPercentOfExamplesCoveredByRightRule());
		writer.println();
		writer.println("Rules:");
		// Write rules
		for (int i = 0; i < statistic.getRules().size(); i++) {
			SurvivalActionRule rule = statistic.getRules().get(i);
			writer.println("r" + (i+1) +": " + rule);
		}
		writer.close();
	}

	private String generateNameOfFile(String additionalInformation, String extension, String outputDir, String exampleSetName) {
		String fileName = String.format("%s%s_%s.%s",
				outputDir, additionalInformation, FilenameUtils.removeExtension(exampleSetName), extension);
		return fileName;
	}

	public ExampleSet getExampleSet(String path, String labelAttrName, String timeAttrName) throws OperatorException, OperatorCreationException {
		ArffExampleSource arffSource = RapidMiner5.createOperator(ArffExampleSource.class);

		arffSource.setParameter(ArffExampleSource.PARAMETER_DATA_FILE, path);
		com.rapidminer.Process process = new com.rapidminer.Process();
		process.getRootOperator().getSubprocess(0).addOperator(arffSource);
		arffSource.getOutputPorts().getPortByName("output")
		.connectTo(process.getRootOperator().getSubprocess(0).getInnerSinks().getPortByIndex(0));
		IOContainer out = process.run();
		ExampleSet exampleSet = out.get(ExampleSet.class, 0);

		// Set attributes
		Attribute survivalStatusAttr = exampleSet.getAttributes().get(labelAttrName);
		exampleSet.getAttributes().setLabel(survivalStatusAttr);
		Attribute survivalTimeAttr = exampleSet.getAttributes().get(timeAttrName);
		exampleSet.getAttributes().setSpecialAttribute(survivalTimeAttr, SurvivalRule.SURVIVAL_TIME_ROLE);

		return exampleSet;
	}

	public List<ParamSetWrapper> getParameterSets(Document doc) {
		List<ParamSetWrapper> paramSets = new ArrayList<>();

		String lineSeparator = System.getProperty("line.separator");
		NodeList paramSetNodes = doc.getElementsByTagName("parameter_set");

		for (int setId = 0; setId < paramSetNodes.getLength(); setId++) {
			ParamSetWrapper wrapper = new ParamSetWrapper();
			Element setNode = (Element) paramSetNodes.item(setId);
			wrapper.name = setNode.getAttribute("name");
			Logger.log("Reading parameter set " + setNode.getAttribute("name")
			+ lineSeparator, Level.INFO);
			NodeList paramNodes = setNode.getElementsByTagName("param");

			for (int paramId = 0; paramId < paramNodes.getLength(); ++paramId) {
				Element paramNode = (Element) paramNodes.item(paramId);
				String name = paramNode.getAttribute("name");
				String value = paramNode.getTextContent();

				if (name.equals(ExperimentExecutor.PARAM_MIN_RULE_COVERED)) {
					if (!StringUtils.isNumber(value)) {
						Logger.log("Value of min_rule_covered is not a number."
								+ lineSeparator, Level.WARNING);
					}
				}
				wrapper.map.put(name, value);
			}
			paramSets.add(wrapper);
		}
		return paramSets;
	}

	public List<DatasetWrapper> getDatasets (Document doc) {
		List<DatasetWrapper> datasets = new ArrayList<>();

		String lineSeparator = System.getProperty("line.separator");
		NodeList datasetNodes = doc.getElementsByTagName("dataset");

		for (int datasetId = 0; datasetId < datasetNodes.getLength(); datasetId++) {
			Logger.log("Processing dataset" + datasetId + lineSeparator, Level.INFO);
			DatasetWrapper wrapper = new DatasetWrapper();
			Element datasetNode = (Element) datasetNodes.item(datasetId);
			if (datasetNode.getElementsByTagName(SurvivalRule.SURVIVAL_TIME_ROLE).getLength() > 0) {
				wrapper.survivalTime = datasetNode.getElementsByTagName(SurvivalRule.SURVIVAL_TIME_ROLE).item(0).getTextContent();
			} else {
				wrapper.survivalTime = null;
			}
			if (datasetNode.getElementsByTagName("label").getLength() > 0) {
				wrapper.label = datasetNode.getElementsByTagName("label").item(0).getTextContent();
			} else {
				wrapper.label = null;
			}
			if (datasetNode.getElementsByTagName("out_directory").getLength() > 0) {
				wrapper.outputDirectory = datasetNode.getElementsByTagName("out_directory").item(0).getTextContent();
			} else {
				wrapper.outputDirectory = ".";
			}
			if (datasetNode.getElementsByTagName("stable_attributes").getLength() > 0) {
				//wrapper.stableAttributes = datasetNode.getElementsByTagName("out_directory").item(0).getTextContent();
				NodeList stableAttributesNodes = datasetNode.getElementsByTagName("stable_attribute");
				wrapper.stableAttributes = new HashSet<String>();
				for (int attrId = 0; attrId < stableAttributesNodes.getLength(); attrId++) {
					Element attrNode = (Element) stableAttributesNodes.item(attrId);
					wrapper.stableAttributes.add(attrNode.getTextContent());
				}
			} else {
				wrapper.stableAttributes = null;
			}

			NodeList trainingNodes = datasetNode.getElementsByTagName("training");
			if(trainingNodes.getLength() == 1){
				Element trainingElement = (Element)trainingNodes.item(0);
				wrapper.datasetDirectory = new ArrayList<String>();
				NodeList trainNodes = trainingElement.getElementsByTagName("train");
				for(int trainId = 0 ; trainId <trainNodes.getLength() ; trainId++ ){
					Element trainNode = (Element)trainNodes.item(trainId);
					String inFile = trainNode.getElementsByTagName("in_file").item(0).getTextContent();
					wrapper.datasetDirectory.add(inFile);
					Logger.log("In file " + inFile + lineSeparator, Level.INFO);
				}
			}
			datasets.add(wrapper);
		}
		return datasets;
	}
}
