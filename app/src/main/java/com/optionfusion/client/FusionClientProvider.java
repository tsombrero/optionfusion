package com.optionfusion.client;

public class FusionClientProvider extends ClientProvider implements ClientProvider.SymbolLookupClientProvider {

    FusionClient client = new FusionClient();

    @Override
    public ClientInterfaces.SymbolLookupClient getSymbolLookupClient() {
        return client;
    }
}
