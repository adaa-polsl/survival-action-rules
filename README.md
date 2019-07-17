# Action rules for survival analysis

This is a JAVA project that can be used for generating action rules based on survival datasets. You can find here also R project, whose purpose is to generate HTML reports with obtained results.

## Java project

Java project is placed in directory *survival-action-rules*. It uses Gradle.

### Building applications from sources

To use this project, you must first add JAR file with project `adaa.analytics.rules` into directory `survival-action-rules/libs`. You can download it from this repository on GitHub - it is located in "releases": https://github.com/adaa-polsl/survival-action-rules/releases/tag/v0.1.

The description of application building will be described with the example of IDE Eclipse.
1. From the `Eclipse Marketplace` you need to install *Buildship Gradle Integration 3.0*.
2. Import the `survival-action-rules` project as a Gradle project.
3. Then open the *Gradle Tasks* view and double-click on the `build` view. After this, the application should be built.
4. You can start the application with the proper arguments. The main class of the application is `pl.polsl.survival.actionrules.ExperimentExecutor`. The argument is a path to an XML file, which contains the configuration of the experiment.

### Input data

Input data, on the basis of which the action rules are generated, are given to the program in ARFF format.
The startup configuration is done using an XML file, which is given as an argument for the main function of the program. The file consists of 2 parts: parameters and data sets. In one file, the user can specify multiple versions of parameters' combinations and multiple data sets. After starting the program, each set is evaluated with each parameter configuration.
The basic structure of the file:

```xml
<?xml version="1.0"?>
<experiment>
  <parameter_sets>
    <parameter_set name="param_1"> ... </parameter_set>
    <parameter_set name="param_1"> ... </parameter_set>
    ...
  </parameter_sets>
  <datasets>
    <dataset> ... </dataset>
    <dataset> ... </dataset>
    ...
  </datasets>
</experiment>
```

The following information can be defined for a given parameter set:
* name of the parameter set,
* minimum number of examples covered by the rule - parameter
`min_rule_covered`, takes a numeric value,
* a parameter that determines whether the target rule should have a better, worse or any other survival curve than the left rule. This is the `target_survival_curve` parameter. It can have the following values:
    + *any* - the survival curve can have any position.
    + *better* - right rule should be the rule of improvement
    + *worse* - right rule should be worsened rule.

An example section of parameters in an XML file is presented below:

```xml
<parameter_set name ="better-target-rule">
  <param name = "min_rule_covered">10</param>
  <param name = "target_survival_curve">better</param>
</parameter_set>
```
The second part of the XML file contains information about the data sets:

```xml
<dataset>
  <label>survival_status</label>
  <survival_time>survival_time</survival_time>
  <out_directory>./result_dir/</out_directory>
  <stable_attributes>
    <stable_attribute>a</stable_attribute>
    <stable_attribute>b</stable_attribute>
    ...
  </stable_attributes>
  <training><train>
    <in_file>dataset.arff</in_file >
  </train></training>
</dataset >
```
The user can define the following information here:
* `label` - the name of the attribute that indicates the survival status,
* `survival_time` - the name of the attribute that indicates the survival time,
* `out_directory` - path to the directory where the results are to be saved for a given dataset,
* `stable_attributes` - a set of stable attributes,
* `in_file` - path to the ARFF file with the dataset.

### Output data

After the experiment, the results are saved in text files and CSV files. They are placed in a directory specified by the parameter `out_directory` in the configuration file. Subdirectories are created in this file, each for a different type of parameter configuration. The subdirectory names respond to the `parameter_set` tags in an XML file.
The results of the experiment are saved in 4 files:

* `result_<datasetName>.txt` - a file contains basic information about the data set and general statistics about the generated rules. There are here, too, the rules themselves.
* `eachRuleStatistic_<datasetName>.csv` - a file with detailed indicators for each rule.
* `estimator_<datasetName>.csv` - a file with Kaplan-Meier estimator values. For the left and right side of each rule, there are survival probabilities for the given times.
* `coveredExamples_<datasetName>.txt` - a file with example ids, which are covered by the left and right rule of each generated action rule, respectively. The first example has the identifier `0`.

## Data availability

* actg320 (HIV-infected patients): [ftp://ftp.wiley.com/public/sci_tech_med/survival](ftp://ftp.wiley.com/public/sci_tech_med/survival)
* BMT-Ch (bone marrow transplant): https://github.com/adaa-polsl/GuideR/blob/master/datasets/bmt/bone-marrow.arff
* cancer (advanced lung cancer patients): survival R package
* follic (follicular cell lymphoma patients): randomForestSRC R package
* GBSG2 (node-positive breast cancer patients): TH.data R package
* hd (Hodgkin's disease patients): randomForestSRC R package
* lung (early detection of lung cancer): [​https://www.stats.ox.ac.uk/pub/datasets/csb](https://www.stats.ox.ac.uk/pub/datasets/csb)
* Melanoma (malignant melanoma patients after radical operation): riskRegression R package
* mgus (patients with monoclonal gammopathy of undetermined significance): survival R package
* pbc (primary biliary cirrhosis of the liver): survival R package
* std (occurrence of sexually transmitted diseases): KMsurv R package
* uis (drug abuse reduction): quantreg R package
* wcgs (occurrence of coronary heart disease): epitools R package
* whas1 (myocardial infarction patients, 1st book edition): [​ftp://ftp.wiley.com/public/sci_tech_med/survival](ftp://ftp.wiley.com/public/sci_tech_med/survival)
* whas500 (myocardial infarction patients, 2nd book edition): [​ftp://ftp.wiley.com/public/sci_tech_med/survival](ftp://ftp.wiley.com/public/sci_tech_med/survival)
* zinc (esophageal cancer): NestedCohort R package

## R package and report generation

A package  `surv.action.rules` written in R is located in the `r-package` directory. It is used to generate HTML files with a visualization of the results obtained during experiments with data.

In order to use this package, you need to have installed:

* [R](https://cran.rstudio.com/)
* [RStudio](https://www.rstudio.com/products/rstudio/download/)

After opening a project in RStudio you should install and build a project (on the right, in the top corner there should be the *Install and Restart* button). Then just run the `generate_reports()` function. Reports for the results in the `results` directory will be created.
