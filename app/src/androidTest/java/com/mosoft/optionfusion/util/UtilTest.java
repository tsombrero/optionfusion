package com.mosoft.optionfusion.util;

import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;

public class UtilTest {

    @Test
    public void testFormatDollars() throws Exception {
        assertEquals(Util.formatDollars(0D), "$0.00");
        assertEquals(Util.formatDollars(0D, 0), "$0");
        assertEquals(Util.formatDollars(1000.23D), "$1,000.23");
        assertEquals(Util.formatDollars(1000.23D, 1000), "$1,000");
        assertEquals(Util.formatDollars(200000000.10D), "$200,000,000.10");
        assertEquals(Util.formatDollars(200000000.10D, 200000001), "$200,000,000.10");
        assertEquals(Util.formatDollars(-10.01D), "-$10.01");
        assertEquals(Util.formatDollars(-10.01D, 10), "-$10");

        assertEquals(Util.formatDollarsCompact(10.00), "$10");
    }

    @Test
    public void testFormatDollarChange() throws Exception {
        assertEquals(Util.formatDollarChange(0D), "0.00");
        assertEquals(Util.formatDollarChange(0D, 0), "0");
        assertEquals(Util.formatDollarChange(1000.23321D), "1,000.23");
        assertEquals(Util.formatDollarChange(1000.23321D, 1000), "1,000");
        assertEquals(Util.formatDollarChange(200000000.100001D), "200,000,000.10");
        assertEquals(Util.formatDollarChange(200000000.101D, 200000001), "200,000,000.10");
        assertEquals(Util.formatDollarChange(-10.01D), "-10.01");
        assertEquals(Util.formatDollarChange(-10.01D, 10), "-10");
    }

    @Test
    public void testFormatPercent() throws Exception {
        assertEquals("0", Util.formatPercent(0D));
        assertEquals("9999x", Util.formatPercent(9999.1D));
        assertEquals("100x", Util.formatPercent(100.1D));
        assertEquals("101%", Util.formatPercent(1.01234D));
        assertEquals("15.1%", Util.formatPercent(0.15123D));
        assertEquals("5.12%", Util.formatPercent(0.05123D));

        assertEquals("-9999x", Util.formatPercent(-9999.1D));
        assertEquals("-100x", Util.formatPercent(-100.1D));
        assertEquals("-101%", Util.formatPercent(-1.01234D));
        assertEquals("-15.1%", Util.formatPercent(-0.15123D));
        assertEquals("-5.12%", Util.formatPercent(-0.05123D));
        assertEquals("-5.10%", Util.formatPercent(-0.05103D));

        assertEquals("1%", Util.formatPercentCompact(0.01001D));
        assertEquals("11%", Util.formatPercentCompact(0.1101D));
        assertEquals("-1%", Util.formatPercentCompact(-0.01001D));
        assertEquals("-11%", Util.formatPercentCompact(-0.1101D));
    }

    @Test
    public void testGetFormattedOptionDate() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, Calendar.JANUARY, 22);
        LocalDate date = new LocalDate(calendar);

        assertEquals("22 Jan", Util.getFormattedOptionDate(date));
        calendar.add(Calendar.YEAR, 1);
        date = new LocalDate(calendar);
        assertEquals("Jan 2017", Util.getFormattedOptionDate(date));
    }

    @Test
    public void testFormatDollarRange() throws Exception {
        assertEquals("All", Util.formatDateRange(null, null));

        Calendar calendarStart = Calendar.getInstance();
        calendarStart.set(2016, Calendar.JANUARY, 22);

        Calendar calendarEnd = Calendar.getInstance();
        calendarEnd.set(2016, Calendar.MARCH, 25);

        assertEquals("Before 25 Mar", Util.formatDateRange(null, new LocalDate(calendarEnd)));
        assertEquals("After 25 Mar", Util.formatDateRange(new LocalDate(calendarEnd), null));
        assertEquals("22 Jan - 25 Mar", Util.formatDateRange(new LocalDate(calendarStart), new LocalDate(calendarEnd)));
    }
}