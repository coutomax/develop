package org.event;

import java.util.HashMap;

public class SpecialKeyAdapter {

    private final HashMap<Integer, Integer> specialKeys = new HashMap<>();

    public SpecialKeyAdapter (){
        initHash();
    }

    private void initHash () {
        specialKeys.put(162,17); //CTRL
        specialKeys.put(163,17); //CTRL
        specialKeys.put(13,10); //ENTER
        specialKeys.put(164,18); //ALT
        specialKeys.put(160,16); //SHIFT
        specialKeys.put(161,16); //SHIFT
        specialKeys.put(188,44); //VIRGULA
        specialKeys.put(191,47); //BARRA
        specialKeys.put(221,91); //ALT GR
        specialKeys.put(220,93); //ALT GR
        specialKeys.put(165,65406); //ALT GR
        specialKeys.put(193,47); // /
        specialKeys.put(192,92); // \
        specialKeys.put(226,92); // \
    }
    public int isSpecialKey (Integer key) {
        return specialKeys.getOrDefault(key, key);
    }

}
