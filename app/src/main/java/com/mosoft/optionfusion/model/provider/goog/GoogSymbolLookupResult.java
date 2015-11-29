package com.mosoft.optionfusion.model.provider.goog;

import android.database.Cursor;
import android.database.MatrixCursor;

import com.google.gson.annotations.SerializedName;
import com.mosoft.optionfusion.client.ClientInterfaces;
import com.mosoft.optionfusion.client.ClientInterfaces.SymbolLookupClient.SuggestionColumns;

import java.util.List;

/*

{
  "matches": [
    {
      "id": "656159",
      "e": "NASDAQ",
      "n": "Whole Foods Market, Inc.",
      "t": "WFM"
    },
    {
      "id": "660073",
      "e": "NYSE",
      "n": "Dean Foods Co",
      "t": "DF"
    },
    ...
  ]
}

 */
public class GoogSymbolLookupResult {
    private List<Match> matches;

    public Cursor getResultCursor() {
        if (matches == null)
            return ClientInterfaces.SymbolLookupClient.EMPTY_CURSOR;

        MatrixCursor ret = new MatrixCursor(SuggestionColumns.getNames());

        for (Match match : matches) {
            ret.newRow()
                    .add(SuggestionColumns._id.name(), 0)
                    .add(SuggestionColumns.symbol.name(), match.ticker)
                    .add(SuggestionColumns.description.name(), match.description);
        }

        return ret;
    }

    private static class Match {
        @SerializedName("n")
        String description;

        @SerializedName("t")
        String ticker;
    }


}
