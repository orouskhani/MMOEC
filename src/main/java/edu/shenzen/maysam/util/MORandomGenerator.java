package edu.shenzen.maysam.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MORandomGenerator {

    private static List<Integer> list;
    private static int counter;

    private MORandomGenerator(){

    }
    public static Integer getNextRandomFromList(int size){
        if(list == null){
            counter = -1;
            list = new ArrayList<>();
            for (int i=0; i<size; i++) {
                list.add(i);
            }
            Collections.shuffle(list);
        }
        counter += 1;
        return list.get(counter);
    }

}
