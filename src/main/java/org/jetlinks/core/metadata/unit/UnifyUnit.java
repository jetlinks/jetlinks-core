package org.jetlinks.core.metadata.unit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;


/**
 * 统一单位
 *
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum UnifyUnit implements StandardUnit, EnumDict<String> {

    //常用单位
    percent("百分比", "%", "common", "百分比(%)"),
    count("次", "count", "common", "次"),
    turnPerSeconds("转每分钟", "turn/m", "common", "转每分钟"),

    //计量单位
    //=====https://baike.baidu.com/item/%E4%B8%AD%E5%9B%BD%E6%B3%95%E5%AE%9A%E8%AE%A1%E9%87%8F%E5%8D%95%E4%BD%8D/662681#1_1=======
    //==================长度(length)单位===================
    nanometer("纳米", "nm", "length", "长度单位:纳米(nm)"),
    micron("微米", "μm", "length", "长度单位:微米(μm)"),
    millimeter("毫米", "mm", "length", "长度单位:毫米(mm)"),
    centimeter("厘米", "cm", "length", "长度单位:厘米(cm)"),
    meter("米", "m", "length", "长度单位:米(m)"),
    kilometer("千米", "km", "length", "长度单位:千米(km)"),

    //==================面积(area)单位===================
    squareMillimeter("平方毫米", "mm²", "area", "面积单位:平方毫米(mm²)"),
    squareCentimeter("平方厘米", "cm²", "area", "面积单位:平方厘米(cm²)"),
    squareMeter("平方米", "m²", "area", "面积单位:平方米(m²)"),
    squareKilometer("平方千米", "km²", "area", "面积单位:平方千米(km²)"),
    hectare("公顷", "hm²", "area", "面积单位:公顷(hm²)"),


    //==================时间(time)单位===================

    days("天", "d", "time", "时间单位:天(d)"),
    hour("小时", "h", "time", "时间单位:小时(h)"),
    minutes("分钟", "min", "time", "时间单位:分钟(m)"),
    seconds("秒", "s", "time", "时间单位:秒(s)"),
    milliseconds("毫秒", "ms", "time", "时间单位:毫秒(ms)"),
    microseconds("微秒", "μs", "time", "时间单位:微秒(μs)"),
    nanoseconds("纳秒", "ns", "time", "时间单位:纳秒(ns)"),


    //==================体积(volume)单位===================
    cubicMillimeter("立方毫米", "mm³", "volume", "体积单位:立方毫米(mm³)"),
    cubicCentimeter("立方厘米", "cm³", "volume", "体积单位:立方厘米(cm³)"),
    cubicMeter("立方米", "m³", "volume", "体积单位:立方米(m³)"),
    cubicKilometer("立方千米", "km³", "volume", "体积单位:立方千米(km³)"),


    //==================容积(capacity)单位===================
    milliliter("毫升", "mL", "capacity", "容积单位:毫升(mL)"),
    litre("升", "L", "capacity", "容积单位:升(L)"),

    //==================质量(mass)单位===================
    milligram("毫克", "mg", "mass", "重量单位:毫克(mg)"),
    gramme("克", "g", "mass", "重量单位:克(g)"),
    kilogram("千克", "kg", "mass", "重量单位:千克(kg)"),
    ton("吨", "t", "mass", "重量单位:吨(t)"),

    //==================力(force)单位 ====================
    newton("牛顿", "N", "force", "力单位:牛顿(N)"),

    //==================压力(pressure)单位===================
    pascal("帕斯卡", "Pa", "pressure", "压力单位:帕斯卡(Pa)"),
    kiloPascal("千帕斯卡", "kPa", "pressure", "压力单位:千帕斯卡(kPa)"),

    //==================温度(temperature)单位===================
    kelvin("开尔文", "K", "temperature", "温度单位:开尔文(K)"),
    celsiusDegrees("摄氏度", "℃", "temperature", "温度单位:摄氏度(℃)"),
    fahrenheit("华氏度", "℉", "temperature", "温度单位:华氏度(℉)"),

    //==================能(energy)单位 ====================
    joule("焦耳", "J", "pressure", "能单位:焦耳(J)"),
    electronVolts("电子伏", "eV", "pressure", "能单位:电子伏(eV)"),
    kWattsHour("千瓦·时", "kW·h", "pressure", "能单位:千瓦·时(kW·h)"),
    cal("卡", "cal", "pressure", "能单位:卡(cal)"),


    //==================功率(power)单位===================
    watt("瓦特", "W", "power", "功率单位:瓦特(W)"),
    kilowatt("千瓦特", "kW", "power", "功率单位:千瓦特(kW)"),

    //==================角度(angle)单位===================
    radian("弧度", "rad", "angle", "角度单位:弧度(rad)"),
    degrees("度", "°", "angle", "角度单位:度(°)"),
    fen("[角]分", "′", "angle", "角度单位:分(′)"),
    angleSeconds("[角]秒", "″", "angle", "角度单位:度(″)"),


    //==================频率(frequency)单位===================

    hertz("赫兹", "Hz", "frequency", "频率单位:赫兹(Hz)"),
    megahertz("兆赫兹", "MHz", "frequency", "频率单位:兆赫兹(MHz)"),
    ghertz("G赫兹", "GHz", "frequency", "频率单位:G赫兹(GHz)"),

    //==================速度(speed)单位===================

    mPerSec("米每秒", "m/s", "speed", "速度单位:米每秒(m/s)"),
    kmPerHr("千米每小时", "km/h", "speed", "速度单位:千米每小时(km/h)"),
    knots("节", "kn", "speed", "速度单位:节(kn)"),
    ;

    private final String name;

    private final String symbol;

    private final String type;

    private final String description;

    @Override
    public String getId() {
        return name();
    }

    @Override
    public String format(Object value) {
        return String.format("%s%s", value, getSymbol());
    }

    static Function<Object, String> template(String strTemplate) {
        return o -> String.format(strTemplate, o);
    }

    public static UnifyUnit of(String value) {
        return Stream.of(UnifyUnit.values())
                .filter(unifyUnit -> unifyUnit.getId().equals(value) || unifyUnit.getSymbol().equals(value))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getText() {
        return getName().concat("(").concat(getSymbol()+")");
    }

    @Override
    public Object getWriteJSONObject() {
        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put("id", this.getValue());
        jsonObject.put("value", this.getValue());
        jsonObject.put("text", this.getText());
        jsonObject.put("symbol", this.getSymbol());
        jsonObject.put("name", this.getName());
        jsonObject.put("type", this.getType());
        jsonObject.put("description", this.getDescription());
        return jsonObject;
    }
}
