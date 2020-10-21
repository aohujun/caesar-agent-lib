package com.hupu.msv.apm.plugin.gray.common.strategy.route;

import java.util.List;
import java.util.Random;

/**
 * @description: ����Ȩ��
 * @author: Aile
 * @create: 2019/09/10 18:59
 */
public class WeightUtils {

    public static int choose(List<Integer> weightSoFars) {
        if (weightSoFars == null || weightSoFars.isEmpty()) {
            return -1;
        }

        int size = weightSoFars.size();
        if (size == 1) {
            return 0;
        }

        Random random = new Random();
        int randomWeight = random.nextInt(weightSoFars.get(size - 1));

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
