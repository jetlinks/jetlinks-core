
# Jet Links 核心模块
[![Maven Central](https://img.shields.io/maven-central/v/org.jetlinks/jetlinks-core.svg)](http://search.maven.org/#search%7Cga%7C1%7Cjetlinks-core)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v/https/oss.sonatype.org/content/repositories/snapshots/org/jetlinks/jetlinks-core/maven-metadata.xml.svg)](https://oss.sonatype.org/content/repositories/snapshots/org/jetlinks/jetlinks-core)
[![Build Status](https://travis-ci.com/jetlinks/jetlinks-core.svg?branch=master)](https://travis-ci.com/jetlinks/jetlinks-core)
[![codecov](https://codecov.io/gh/jetlinks/jetlinks-core/branch/master/graph/badge.svg)](https://codecov.io/gh/jetlinks/jetlinks-core)

JetLinks Protocol（以下简称协议）将设备定义和对设备的操作抽象为一系列通用接口，实现对多种协议的支持和转换，
业务系统只需要关心设备以及设备能做的事，不用再关心具体的协议细节。从APP到服务内部，使用同一套API，
最终让设备端做最小的改动即可接入平台。

# 设备定义

设备主要由3部分组成：
1. 属性，对设备的描述，如： 型号，当前电量。
2. 功能，对设备的操作，如： 打开开关，获取设备状态。
3. 事件，设备主动上报数据，如：定时上报温度，传感器触发警报。

## 数据类型
TODO