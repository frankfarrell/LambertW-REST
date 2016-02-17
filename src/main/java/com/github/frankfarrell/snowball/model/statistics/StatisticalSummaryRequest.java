package com.github.frankfarrell.snowball.model.statistics;

import com.github.frankfarrell.snowball.model.WorkOrderClass;

import java.util.List;

/**
 * Created by Frank on 14/02/2016.
 */

//TODO Filters not required
public class StatisticalSummaryRequest {

    private List<WorkOrderClass> filters; //eg filter by type
    private List<StatisticType> statistics; //What statistics wanted

    public StatisticalSummaryRequest() {
    }

    public List<WorkOrderClass> getFilters() {
        return filters;
    }

    public void setFilters(List<WorkOrderClass> filters) {
        this.filters = filters;
    }

    public List<StatisticType> getStatistics() {
        return statistics;
    }

    public void setStatistics(List<StatisticType> statistics) {
        this.statistics = statistics;
    }
}
