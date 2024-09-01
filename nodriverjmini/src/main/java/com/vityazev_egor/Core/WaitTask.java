package com.vityazev_egor.Core;

// TODO избавиться от дополнительного абстрактного класса. Можно просто сделать сам класс WaitTask абстракным с одним методом, который надо переоределить
public class WaitTask {
    public static abstract class IWaitTask {
        public abstract Boolean execute();
    }

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
