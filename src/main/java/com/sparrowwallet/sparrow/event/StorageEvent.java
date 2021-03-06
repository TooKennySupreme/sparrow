package com.sparrowwallet.sparrow.event;

import com.sparrowwallet.sparrow.io.Config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class StorageEvent extends TimedEvent {
    private static boolean firstRunDone = false;
    private static final Map<File, Long> eventTime = new HashMap<>();

    public StorageEvent(File file, Action action, String status) {
        super(action, status);

        Integer keyDerivationPeriod = Config.get().getKeyDerivationPeriod();
        if(keyDerivationPeriod == null) {
            keyDerivationPeriod = -1;
        }

        if(action == Action.START) {
            eventTime.put(file, System.currentTimeMillis());
            timeMills = keyDerivationPeriod;
        } else if(action == Action.END) {
            long start = eventTime.get(file);
            if(firstRunDone) {
                keyDerivationPeriod = (int)(System.currentTimeMillis() - start);
                Config.get().setKeyDerivationPeriod(keyDerivationPeriod);
            }
            firstRunDone = true;
            timeMills = 0;
        }
    }
}
