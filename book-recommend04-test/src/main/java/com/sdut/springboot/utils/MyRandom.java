package com.sdut.springboot.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MyRandom {

    /**
     * 获取0~max范围内不重复的随机数
     * @param num   要生成几个随机数
     * @param max   最大值 左闭右开
     * @return
     */
    public static List<Integer> random(int num,int max){

        List<Integer> randoms = new ArrayList<>();
        int count=0;
        while(num!=count){
            int flag=1;
            int ran = new Random().nextInt(max);

            for (Integer random:randoms){
                if(ran==random){
                    flag=0;
                    break;
                }
            }
            if(flag==1){
                randoms.add(ran);
                count++;
            }
        }
        return randoms;
    }

}
