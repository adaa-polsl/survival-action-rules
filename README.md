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

## R package and report generation

A package  `surv.action.rules` written in R is located in the `r-package` directory. It is used to generate HTML files with a visualization of the results obtained during experiments with data.

In order to use this package, you need to have installed:

* [R](https://cran.rstudio.com/)
* [RStudio](https://www.rstudio.com/products/rstudio/download/)

After opening a project in RStudio you should install and build a project (on the right, in the top corner there should be the *Install and Restart* button). Then just run the `generate_reports()` function. Reports for the results in the `results` directory will be created.
