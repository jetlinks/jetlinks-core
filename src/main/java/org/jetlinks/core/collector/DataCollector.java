package org.jetlinks.core.collector;

import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.monitor.Monitor;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.function.Consumer;

public interface DataCollector {

    String getId();

    String getName();

    Mono<DataCollector.ChannelRuntime> createChannel(ChannelConfiguration context);

    Mono<DataCollector.ChannelRuntime> reloadChannel(DataCollector.ChannelRuntime runtime,
                                                     ChannelConfiguration context);

    interface ChannelConfiguration {

        /**
         * 获取通道配置
         *
         * @return 通道配置
         */
        ChannelProperties getProperties();

        /**
         * 获取采集器管理器
         *
         * @return 采集器管理器
         */
        DataCollectorManager getManager();

        /**
         * 创建通道监控器
         *
         * @return 监控器
         */
        Monitor createMonitor();

        /**
         * 创建采集器监控器
         *
         * @param collectorId 采集器ID
         * @return 监控器
         */
        Monitor createMonitor(String collectorId);

        /**
         * 创建点位监控器,用于在执行点位相关操作时进行监控.如打印日志,链路追踪,熔断等操作.
         *
         * @param collectorId 采集器ID
         * @param pointId     点位ID
         * @return 监控器
         */
        Monitor createMonitor(String collectorId, String pointId);

    }


    interface ChannelRuntime extends Lifecycle, CommandSupport {

        /**
         * 获取通道ID
         *
         * @return 通道ID
         */
        String getId();

        /**
         * 注册采集器
         *
         * @param properties 采集器配置
         * @return void
         */
        Mono<Void> registerCollector(Collection<CollectorProperties> properties);

        /**
         * 注销采集器
         *
         * @param id 采集器ID
         * @return void
         */
        Mono<Void> unregisterCollector(Collection<String> id);

        /**
         * 获取指定ID的采集器运行时
         *
         * @param id 采集器ID
         * @return 采集器信息
         */
        Mono<CollectorRuntime> getCollector(String id);

        /**
         * 获取指定ID的采集器信息
         *
         * @param id 采集器ID
         * @return 采集器信息
         * @see CollectorProperties#getId()
         */
        Flux<CollectorRuntime> getCollectors(Collection<String> id);

    }


    interface CollectorRuntime extends Lifecycle, CommandSupport {

        /**
         * 获取采集器ID
         *
         * @return 采集器ID
         */
        String getId();

        /**
         * 注册点位
         *
         * @param points 点位信息
         * @return 注销点位
         */
        Mono<Void> registerPoint(Collection<PointProperties> points);

        /**
         * 注销点位
         *
         * @param idList 点位ID
         * @return 注销点位
         */
        Mono<Void> unregisterPoint(Collection<String> idList);

        /**
         * 获取指定ID的点位运行时
         *
         * @param id 点位ID
         * @return 点位信息
         */
        Mono<PointRuntime> getPoint(String id);

        /**
         * 获取指定ID的点位信息
         *
         * @param idList 点位ID
         * @return 点位信息
         * @see PointProperties#getId()
         */
        Flux<PointRuntime> getPoints(Collection<String> idList);

        /**
         * 获取全部点位信息
         *
         * @return 点位信息
         */
        Flux<PointRuntime> getPoints();

        /**
         * 订阅点位数据. 被动接收点位数据.
         *
         * @return 点位数据
         * @see AccessMode#subscribe
         */
        Disposable subscribe(Consumer<PointData> listener);

        /**
         * 采集指定的点位数据.
         *
         * @param idList 点位ID
         * @return 点位数据
         * @see PointRuntime#read()
         * @see AccessMode#read
         */
        Flux<PointData> collect(Collection<String> idList);

    }

    interface PointRuntime extends Lifecycle {

        String getId();

        /**
         * 读取点位数据
         *
         * @return 点位数据
         */
        Mono<PointData> read();

        /**
         * 写入点位数据
         *
         * @param data 点位数据
         * @return 点位数据
         */
        Mono<PointData> write(PointData data);

    }

    interface Lifecycle {

        Mono<State> state();

        Mono<Void> start();

        Mono<Void> shutdown();

    }


    interface State {

        String getValue();

        String getText();

    }
}
