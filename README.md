
# Jet Links 核心模块
[![Maven Central](https://img.shields.io/maven-central/v/org.jetlinks/jetlinks-core.svg)](http://search.maven.org/#search%7Cga%7C1%7Cjetlinks-core)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v/https/oss.sonatype.org/content/repositories/snapshots/org/jetlinks/jetlinks-core/maven-metadata.xml.svg)](https://oss.sonatype.org/content/repositories/snapshots/org/jetlinks/jetlinks-core)
[![Build Status](https://travis-ci.com/jetlinks/jetlinks-core.svg?branch=master)](https://travis-ci.com/jetlinks/jetlinks-core)
[![codecov](https://codecov.io/gh/jetlinks/jetlinks-core/branch/master/graph/badge.svg)](https://codecov.io/gh/jetlinks/jetlinks-core)


# 设备定义(metadata)

设备主要由3部分组成：
1. 属性，对设备的描述，如： 型号，当前电量。
2. 功能，对设备的操作，如： 打开开关，获取设备状态。
3. 事件，设备主动上报数据，如：定时上报温度，传感器触发警报。

## 数据类型


# 设备注册中心(registry)
负责管理设备到基础信息,配置,状态以及集群下到消息收发.

```java
   DeviceRegistry registry  = ....;

   //发送调用设备功能消息到设备并等待返回
   DeviceSysInfo output= registry.getDevice(deviceId)
          .messageSender()
          .invokeFunction("getSysInfo")
          .tryValidateAndSend(10,TimeUnit.SECONDS)//最大等待10秒
          //超时异常处理
          .recover(TimeoutException.class, err -> FunctionInvokeMessageReply.create().error(ErrorCode.TIME_OUT))
          .map(this::convertSysInfo)
          .get(); 
          
```

# 多协议支持(protocol)
平台支持多消息协议支持,使用不同消息协议(ALink,MIot....)的设备只需要做最小改动(修改服务器地址和证书)即可接入.
对平台其他服务无侵入.
