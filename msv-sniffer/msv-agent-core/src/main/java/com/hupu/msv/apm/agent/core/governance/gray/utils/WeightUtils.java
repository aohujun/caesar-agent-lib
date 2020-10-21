package com.hupu.msv.apm.agent.core.governance.gray.utils;

import java.util.List;
import java.util.Random;

/**
 * @description: 计算权重
 * @author: Aile
 * @create: 2019/09/10 18:59
 */
public class WeightUtils {

    private static Random random = new Random();

    public static int choose(List<Integer> weightSoFars) {
        if (weightSoFars == null || weightSoFars.isEmpty()) {
            return -1;
        }

        if (weightSoFars.size() == 1) {
            return 0;
        }

        int randomWeight = random.nextInt(weightSoFars.get(weightSoFars.size() - 1));

        int index = -1;
        for (int weight : weightSoFars) {
            index++;
            if (weight > randomWeight) {
                return index;
            }
        }

        return index;
    }


}
