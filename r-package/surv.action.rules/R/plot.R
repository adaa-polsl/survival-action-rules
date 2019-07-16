#' Plot conlusion of survival action rule
#'
#' @param estimator Data frame with columns: "time", "entire-set", estimators for left rule, estimators for right rule
#' @param x_axis_column Name of a column, that should be on X axis of a plot
#' @param legend_title Name for a legend
#' @param xlab Label for X axis
#' @param ylab Label for Y axis
#' @param title Title of a plot
#' @param labels_legend Names of legends elements
#'
#' @return ggplot
#' @export
plot_km <- function(estimator,
                    x_axis_column = "time",
                    legend_title = "Rule",
                    xlab = "Time",
                    ylab = "Probability",
                    title = "Plot for survival rules",
                    labels_legend = NULL) {
  df <- reshape2::melt(estimator, id.vars = x_axis_column)
  p <- ggplot(data = df, aes(x = time, y = value, col = variable)) +
    geom_step() +
    labs(color = legend_title) +
    xlab(xlab) +
    ylab(ylab) +
    ggtitle(title) +
    ylim(c(0,1))
  if (is.null(labels_legend)) {
    p <- p + scale_color_manual(values=c("#636363", "#e41a1c", "#4daf4a"))
  } else {
    p <- p + scale_color_manual(values=c("#636363", "#e41a1c", "#4daf4a"),
                           labels = labels_legend)
  }
  return(p)
}

# plot_km_specific_rule <- function(nb_of_rule,
#                                   estimator,
#                                   x_axis_column = "time",
#                                   legend_title = "Rule",
#                                   xlab = "Time",
#                                   ylab = "Probability",
#                                   title = "Plot for survival rules",
#                                   labels_legend = NULL) {
#   est <- estimator[, c(1, 2, 2*nb_of_rule+1, 2*nb_of_rule+2)]
#   plot_km(est,
#           x_axis_column = x_axis_column,
#           legend_title = legend_title,
#           xlab = xlab,
#           ylab = ylab,
#           title = title,
#           labels_legend = labels_legend)
# }

#' Generate plot for conclusions of every rule
#'
#' @param estimator Dataframe with KM estimators
#'
#' @return List of plots
#' @export
plot_km_each_rule <- function(estimator) {
  nb_of_rules <- (ncol(estimator) - 2) / 2 # first two columns are "time" and "entire-set"
  list_plot <- list()
  for (i in 1:nb_of_rules) {
    tmp_estimator <- estimator[, c(1, 2, 2*i+1, 2*i+2)]
    plot <- plot_km(tmp_estimator, title = paste0("r", i))
    list_plot <- append(list_plot, list(plot))
  }
  return(list_plot)
}

# two_histograms <- function(left_vec, right_vec, bins = 10, attr_name) {
#   df <- data.frame("reguła" = c(rep("lewa", length(left_vec)),
#                             rep("prawa", length(right_vec))),
#                    value = c(left_vec , right_vec ))
#   plot <- easyGgplot2::ggplot2.histogram(data = df,
#                                  xName = "value",
#                                  groupName = "reguła",
#                                  legendPosition="top",
#                                  groupColors=c(
#                                    adjustcolor("#e41a1c", alpha.f = 0.5),
#                                    adjustcolor("#4daf4a", alpha.f = 0.5)),
#                                  bins = 20,
#                                  addMeanLine = TRUE,
#                                  meanLineColor = c("#e41a1c", "#2a9127"),
#                                  scale = "density")
#
#   ggplot(df, aes(x=value,fill=reguła))+
#     geom_histogram(aes(y=5*..density..),
#                    alpha=0.5,position='identity',binwidth=5)
#   ggplot2.customize(plot, xtitle = attr_name, ytitle = "Gęstość",
#                     show.legend = TRUE, legendPosition="top",
#                     mainTitle=paste0("'",
#                                      attr_name,
#                                      "'"), alpha = 0.5)
# }
