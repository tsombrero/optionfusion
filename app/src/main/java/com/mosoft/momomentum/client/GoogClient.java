package com.mosoft.momomentum.client;

import com.mosoft.momomentum.model.provider.Interfaces;

import java.util.List;

public class GoogClient implements ClientInterfaces.OptionChainClient {

    @Override
    public void logIn(String userId, String password, ClientInterfaces.Callback<ClientInterfaces.LoginResponse> callback) {
        // n/a
    }

    @Override
    public void getOptionChain(String symbol, ClientInterfaces.Callback<Interfaces.OptionChain> callback) {

    }

    @Override
    public void getAccounts(ClientInterfaces.Callback<List<? extends Interfaces.Account>> callback) {
        // n/a
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }
}
