#' Generate reports for results of every experiment
#'
#' This function will generate report for every experiment made for induction
#' of survival action rules.
#'
#' @param rmd Path to RMarkdown document
#' @param arff_file_dir Path to a directory with datasets (arff files)
#' @param main_dir Path to a directory with results of all experiments
#'
#' @return None
#' @export
generate_reports <- function(rmd = "./inst/rmd/experiment_result.Rmd",
                             arff_file_dir = "../../datasets/",
                             main_dir = "../../results/") {
  directories <- list.dirs(main_dir, recursive = FALSE)

  for(dataset_dir in directories) {
    gc()
    parameters <- list.dirs(dataset_dir, recursive = FALSE)
    for (param_dir in parameters) {
      title <-  paste0(basename(dataset_dir),
                       " - '",
                       basename(param_dir), "'")
      rmarkdown::render(rmd,
                        output_format = "html_document",
                        output_dir = "../../results/",
                        output_file = paste0(basename(dataset_dir),
                                             "_",
                                             basename(param_dir), ".html"),
                        params = list(param_dir = param_dir,
                                      dataset_dir = dataset_dir,
                                      title = title),
                        knit_root_dir = getwd())
    }
  }
}
