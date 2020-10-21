package com.navercorp.pinpoint.bootstrap.context;

/**
 * @author: edison.li
 * @date: 2020/6/4 3:35 下午
 * @description:
 */
public interface ParsingResult {

    int ID_NOT_EXIST = 0;

    String getSql();

    String getOutput();

    int getId();
}
