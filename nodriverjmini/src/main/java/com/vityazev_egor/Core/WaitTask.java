package com.vityazev_egor.Core;

@Deprecated
public abstract class WaitTask {
    public abstract Boolean condition();

    public static class TimeOutException extends Exception {
        public TimeOutException(String message) {
            super(message);
        }
    }

    public Boolean execute(Integer timeOutSeconds, Integer delayMilis){
        long startTime = System.currentTimeMillis() / 1000;
        while (true) {
            Boolean result = condition();
            if (result){
                return true;
            }
            if ((System.currentTimeMillis()/1000) - startTime >=timeOutSeconds) break;
            Shared.sleep(delayMilis);
        }
        return false;
    }
}
