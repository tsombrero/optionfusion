package com.optionfusion.jobqueue;

import com.birbit.android.jobqueue.Params;

public class SetUserDataJob extends BaseApiJob {
    private final String key;
    private final String value;

    public SetUserDataJob(String key, String value) {
        super(new Params(1)
                .requireNetwork()
                .setPersistent(false)
                .setGroupId(SetUserDataJob.class.getSimpleName()));
        this.key = key;
        this.value = value;
    }

    @Override
    public void onRun() throws Throwable {
        super.onRun();
        accountClient.setUserData(key, value);
    }
}
