package org.jetlinks.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UnitType {

    // TODO: 2019/3/12 自定义单位考虑、接口？
    PERCENTAGE("1","百分比","percentage"),
    CELSIUS("2","摄氏度","celsius"),
    SECONDS("1","秒","seconds"),
    MINUTES("1","分","minutes"),
    HOURS("1","小时","hours"),
    DAYS("1","天","days"),
    KELVIN("1","开氏温度","kelvin"),
    PASCAL("1","帕斯卡","pascal"),
    ARCDEGRESS("1","弧度","arcdegrees"),
    RGB("1","RGB","rgb"),
    WATT("1","瓦特（功率）","watt"),
    LITRE("1","升","litre"),
    PPM("1","ppm浓度","ppm"),
    LUX("1","勒克斯(照度)","lux"),
    MG_M3("1","勒克斯(照度)","mg/m3");

    private String id;

    private String text;

    private String value;
}
