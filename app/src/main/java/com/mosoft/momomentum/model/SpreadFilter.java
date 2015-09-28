package com.mosoft.momomentum.model;

import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.util.Util;

public class SpreadFilter {
    protected Double minAnnualizedReturn;
    protected Double minDaysUntilExpire;
    protected Double maxDaysUntilExpire;
    protected Double minChangeTolerance_MaxReturn;
    protected Double minChangeTolerance_BreakEven;

    public boolean pass(Spread spread) {
        if (spread == null)
            return false;

        if (minAnnualizedReturn != null && spread.getMaxReturnAnnualized() < minAnnualizedReturn)
            return false;

        if (minDaysUntilExpire != null && spread.getDaysToExpiration() < minDaysUntilExpire)
            return false;

        if (maxDaysUntilExpire != null && spread.getDaysToExpiration() > maxDaysUntilExpire)
            return false;

        if (minChangeTolerance_BreakEven != null && spread.getPercentChange_BreakEven() < minChangeTolerance_BreakEven)
            return false;

        if (minChangeTolerance_MaxReturn != null && spread.getPercentChange_MaxProfit() < minChangeTolerance_MaxReturn)
            return false;

        return true;
    }

    public void setMinMonthlyReturn(double pct) {
        minAnnualizedReturn = Util.compoundGrowth(pct, 12);
    }

    public void setMinAnnualizedReturn(double pct) {
        minAnnualizedReturn = pct;
    }

    public void setMinDaysUntilExpire(double minDaysUntilExpire) {
        this.minDaysUntilExpire = minDaysUntilExpire;
    }

    public void setMaxDaysUntilExpire(double maxDaysUntilExpire) {
        this.maxDaysUntilExpire = maxDaysUntilExpire;
    }

    public void setMinChangeTolerance_MaxReturn(double minChangeTolerance_MaxReturn) {
        this.minChangeTolerance_MaxReturn = minChangeTolerance_MaxReturn;
    }

    public void setMinChangeTolerance_BreakEven(double minChangeTolerance_BreakEven) {
        this.minChangeTolerance_BreakEven = minChangeTolerance_BreakEven;
    }
}
