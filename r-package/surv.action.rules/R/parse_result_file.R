#' Get text of a rules
#'
#' @param result_file Path to a file with results
#'
#' @return Text with rules
#' @export
get_rules_text <- function(result_file) { ###########
  result_lines <- readLines(result_file)
  rules <- result_lines[(which(result_lines == "Rules:") + 1):length(result_lines)]
}

#' Get dataframe with rules
#'
#' @param result_file Path to a file with results
#'
#' @return Dataframe with rules
#' @export
get_rules <- function(result_file) {
  rules <- get_rules_text(result_file)
  ids <- lapply(rules, function(x) {substr(x, regexpr("r", x), regexpr(":", x) - 1)}) %>% unlist()
  conditions <- lapply(rules, function(x) {substr(x, regexpr("IF ", x) + 3, regexpr("THEN", x) - 2)}) %>% unlist()
  decisions <- lapply(rules, function(x) {substring(x, regexpr("THEN", x) + 5)}) %>% unlist()
  df <- data.frame(id = ids, premise = conditions, decision = decisions)
  return(df)
}

#' Get statistics of a set of survival action rules
#'
#' @param result_file Path to a file with results
#'
#' @return Dataframe with statistics
#' @export
get_whole_set_stats <- function(result_file) {
  result_lines <- readLines(result_file)
  stats <- result_lines[1:(which(result_lines == "Rules:") - 2)]
  stat_name <- sapply(stats, function(x) {sub(":.*", "", x)})
  stat_value <- sapply(stats, function(x) {sub(".*: ", "", x)})
  df <- data.frame(name = stat_name, value = stat_value)
  return(df)
}

#' Get statistics of attributes for covered examples for every rule
#'
#' @param covering_file Path to a file with information about covered examples
#' @param arff_file Path to a dataset file (arff file)
#' @param rules Dataframe with rules
#'
#' @return Statistics of attributes
#' @export
get_covering <- function(covering_file, arff_file, rules) {
  data <- farff::readARFF(arff_file, show.info = FALSE)
  lines <- readLines(covering_file)
  attr_covered <- list()
  for (l in lines) {
    id <- substr(l, regexpr("r", l), regexpr(":", l) - 1)
    cat(id)
    rule_id <- substr(id, regexpr("r", id), regexpr("_", id) - 1)
    premise <- rules %>% filter(id == rule_id) %>% select(premise) %>%
      unlist() %>% as.character()
    covered_examples <- substr(l, regexpr("\\[", l) + 1, regexpr("\\]", l) - 1) %>%
      strsplit(",") %>% unlist() %>% as.numeric()
    attr_summary <- get_attr_stat(data, covered_examples, premise)
    attr_covered <- append(attr_covered, list(attr_summary))
  }
  names(attr_covered) <- substr(lines, regexpr("r", lines), regexpr(":", lines) - 1)
  return(attr_covered)
}

#' Get statistics of attributes for covered examples for specific rule
#'
#' @param data Dataframe with a dataset
#' @param covered_examples List of covered examples
#' @param premise Premise of a rule
#'
#' @return Dataframe with statistics
#' @export
get_attr_stat <- function(data, covered_examples, premise) {
  attr <- which(sapply(colnames(data), function(x) {grepl(x, premise)}))
  data_part <- data[covered_examples + 1, ] %>% select(attr)

  data_part <- data_part %>% dplyr::select_if(is.numeric)

  sums <- data_part %>% lapply(
    function(x) {
      list(min = min(x),
           per_10 = quantile(x, probs = 0.1, na.rm = TRUE),
           Q1 = quantile(x, probs = 0.25, na.rm = TRUE),
           median = median(x),
           mean = mean(x),
           Q3 = quantile(x, probs = 0.75, na.rm = TRUE),
           per_90 = quantile(x, 0.9, na.rm = TRUE),
           max = max(x))
    }) %>% dplyr::bind_rows()
  sums <- cbind(name = colnames(data_part), sums)
  return(sums)
}
