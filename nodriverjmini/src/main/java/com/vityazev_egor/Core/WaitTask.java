package com.vityazev_egor.Core;

public class WaitTask {
    private final IWaitTask task;
    public WaitTask(IWaitTask task){
        this.task = task;
    }

    public Boolean execute(Integer timeOutSeconds, Integer delayMilis){
        long startTime = System.currentTimeMillis() / 1000;
        while (true) {
            Boolean result = task.execute();
            if (result){
                return true;
            }
            if ((System.currentTimeMillis()/1000) - startTime >=timeOutSeconds) break;
            Shared.sleep(delayMilis);
        }
        return false;
    }
}
