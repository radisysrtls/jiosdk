package com.jio.rtls.sdk.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class JioLocateWorker extends Worker {

    private Context mContext;

    public JioLocateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.mContext = context;

    }

    @NonNull
    @Override
    public Result doWork() {
        makeLocationApiCall();
        return Result.success();
    }

    private void makeLocationApiCall() {

    }
}
