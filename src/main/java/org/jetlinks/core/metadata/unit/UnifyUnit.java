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
public enum UnifyUnit implements ValueUnit, EnumDict<String> {

    //常用单位
    percent("百分比", "%", "常用单位", "百分比(%)"),
    count("次", "count", "常用单位", "次"),
    turnPerSeconds("转每分钟", "r/min", "常用单位", "转每分钟"),

    //计量单位
    //=====https://baike.baidu.com/item/%E4%B8%AD%E5%9B%BD%E6%B3%95%E5%AE%9A%E8%AE%A1%E9%87%8F%E5%8D%95%E4%BD%8D/662681#1_1=======
    //==================长度(length)单位===================
    nanometer("纳米", "nm", "长度单位", "长度单位:纳米(nm)"),
    micron("微米", "μm", "长度单位", "长度单位:微米(μm)"),
    millimeter("毫米", "mm", "长度单位", "长度单位:毫米(mm)"),
    centimeter("厘米", "cm", "长度单位", "长度单位:厘米(cm)"),
    meter("米", "m", "长度单位", "长度单位:米(m)"),
    kilometer("千米", "km", "长度单位", "长度单位:千米(km)"),

    //==================面积(area)单位===================
    squareMillimeter("平方毫米", "mm²", "面积单位", "面积单位:平方毫米(mm²)"),
    squareCentimeter("平方厘米", "cm²", "面积单位", "面积单位:平方厘米(cm²)"),
    squareMeter("平方米", "m²", "面积单位", "面积单位:平方米(m²)"),
    squareKilometer("平方千米", "km²", "面积单位", "面积单位:平方千米(km²)"),
    hectare("公顷", "hm²", "面积单位", "面积单位:公顷(hm²)"),


    //==================时间(time)单位===================

    days("天", "d", "时间单位", "时间单位:天(d)"),
    hour("小时", "h", "时间单位", "时间单位:小时(h)"),
    minutes("分钟", "min", "时间单位", "时间单位:分钟(m)"),
    seconds("秒", "s", "时间单位", "时间单位:秒(s)"),
    milliseconds("毫秒", "ms", "时间单位", "时间单位:毫秒(ms)"),
    microseconds("微秒", "μs", "时间单位", "时间单位:微秒(μs)"),
    nanoseconds("纳秒", "ns", "时间单位", "时间单位:纳秒(ns)"),


    //==================体积(volume)单位===================
    cubicMillimeter("立方毫米", "mm³", "体积单位", "体积单位:立方毫米(mm³)"),
    cubicCentimeter("立方厘米", "cm³", "体积单位", "体积单位:立方厘米(cm³)"),
    cubicMeter("立方米", "m³", "体积单位", "体积单位:立方米(m³)"),
    cubicKilometer("立方千米", "km³", "体积单位", "体积单位:立方千米(km³)"),

    //==================流量单位==================
    cubicMeterPerSec("立方米每秒","m³/s","流量单位","流量单位:立方米每秒(m³/s)"),
    cubicKilometerPerSec("立方千米每秒","km³/s","流量单位","流量单位:立方千米每秒(km³/s)"),
    cubicCentimeterPerSec("立方厘米每秒","cm³/s","流量单位","流量单位:立方厘米每秒(cm³/s)"),
    litrePerSec("升每秒","l/s","流量单位","流量单位:升每秒(l/s)"),

    cubicMeterPerHour("立方米每小时","m³/h","流量单位","流量单位:立方米每小时(m³/h)"),
    cubicKilometerPerHour("立方千米每小时","km³/h","流量单位","流量单位:立方千米每小时(km³/h)"),
    cubicCentimeterPerHour("立方厘米每小时","cm³/h","流量单位","流量单位:立方厘米每小时(cm³/h)"),
    litrePerHour("升每小时","l/h","流量单位","流量单位:升每小时(l/h)"),


    //==================容积(capacity)单位===================
    milliliter("毫升", "mL", "容积单位", "容积单位:毫升(mL)"),
    litre("升", "L", "容积单位", "容积单位:升(L)"),

    //==================质量(mass)单位===================
    milligram("毫克", "mg", "质量单位", "重量单位:毫克(mg)"),
    gramme("克", "g", "质量单位", "重量单位:克(g)"),
    kilogram("千克", "kg", "质量单位", "重量单位:千克(kg)"),
    ton("吨", "t", "质量单位", "重量单位:吨(t)"),

    //==================压力(pressure)单位===================
    pascal("帕斯卡", "Pa", "压力单位", "压力单位:帕斯卡(Pa)"),
    kiloPascal("千帕斯卡", "kPa", "压力单位", "压力单位:千帕斯卡(kPa)"),
    newton("牛顿", "N", "力单位", "力单位:牛顿(N)"),
    newtonMeter("牛·米", "N.m", "力单位", "力单位:牛·米(N.m)"),

    //==================温度(temperature)单位===================
    kelvin("开尔文", "K", "温度单位", "温度单位:开尔文(K)"),
    celsiusDegrees("摄氏度", "℃", "温度单位", "温度单位:摄氏度(℃)"),
    fahrenheit("华氏度", "℉", "温度单位", "温度单位:华氏度(℉)"),

    //==================能(energy)单位 ====================
    joule("焦耳", "J", "能量单位", "能单位:焦耳(J)"),
    cal("卡", "cal", "能量单位", "能单位:卡(cal)"),


    //==================功率(power)单位===================
    watt("瓦特", "W", "功率单位", "功率单位:瓦特(W)"),
    kilowatt("千瓦特", "kW", "功率单位", "功率单位:千瓦特(kW)"),

    //==================角度(angle)单位===================
    radian("弧度", "rad", "角度单位", "角度单位:弧度(rad)"),
    degrees("度", "°", "角度单位", "角度单位:度(°)"),
    fen("[角]分", "′", "角度单位", "角度单位:分(′)"),
    angleSeconds("[角]秒", "″", "角度单位", "角度单位:度(″)"),


    //==================频率(frequency)单位===================

    hertz("赫兹", "Hz", "频率单位", "频率单位:赫兹(Hz)"),
    megahertz("兆赫兹", "MHz", "频率单位", "频率单位:兆赫兹(MHz)"),
    ghertz("G赫兹", "GHz", "频率单位", "频率单位:G赫兹(GHz)"),

    //==================速度(speed)单位===================

    mPerSec("米每秒", "m/s", "速度单位", "速度单位:米每秒(m/s)"),
    kmPerHr("千米每小时", "km/h", "速度单位", "速度单位:千米每小时(km/h)"),
    knots("节", "kn", "速度单位", "速度单位:节(kn)"),


    //==================电(electricity)单位===================

    volt("伏特", "V", "电力单位", "电压:伏特(V)"),
    kiloVolt("千伏", "kV", "电力单位", "电压:千伏(kV)"),
    milliVolt("毫伏", "mV", "电力单位", "电压:毫伏(mV)"),
    microVolt("微伏", "μV", "电力单位", "电压:微伏(μV)"),


    ampere("安培", "A", "电力单位", "电流:安培(A)"),
    milliAmpere("毫安", "mA", "电力单位", "电流:毫安(mA)"),
    microAmpere("微安", "μA", "电力单位", "电流:微安(μA)"),
    nanoAmpere("纳安", "nA", "电力单位", "电流:纳安(nA)"),

    ohm("欧姆","Ω","电力单位","电阻:欧姆(Ω)"),
    kiloOhm("千欧","KΩ","电力单位","电阻:千欧(KΩ)"),
    millionOhm("兆欧","MΩ","电力单位","电阻:兆欧(MΩ)"),
    electronVolts("电子伏", "eV", "电力单位", "能单位:电子伏(eV)"),
    kWattsHour("千瓦·时", "kW·h", "电力单位", "能单位:千瓦·时(kW·h)"),

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
