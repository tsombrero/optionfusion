package com.optionfusion.model.provider.goog;

import com.google.gson.annotations.SerializedName;
import com.optionfusion.client.ClientInterfaces;

import java.util.ArrayList;
import java.util.Collections;
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

    public List<ClientInterfaces.SymbolLookupResult> getResultList() {
        if (matches == null)
            return Collections.EMPTY_LIST;

        List<ClientInterfaces.SymbolLookupResult> lookupResults = new ArrayList<>();

        for (Match match : matches) {
            lookupResults.add(match.asSymbolLookupResult());
        }

        return lookupResults;
    }

    private static class Match {
        @SerializedName("n")
        String description;

        @SerializedName("t")
        String ticker;

        ClientInterfaces.SymbolLookupResult asSymbolLookupResult() {
            return new ClientInterfaces.SymbolLookupResult(ticker, description);
        }
    }
}
