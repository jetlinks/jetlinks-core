package org.jetlinks.core.collector;

import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.monitor.Monitor;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * 针对数据采集的支持,用于平台主动采集的场景,如: 定时采集modbus数据等.
 * <p>
 * 数采集由通道{@link ChannelRuntime}、采集器{@link CollectorRuntime}、点位{@link PointRuntime}组成.
 * <p>
 * 通道 {@link ChannelRuntime} 用于实现一个具体的数据通道，如: 一个modbus主站，一个TCP客户端，一个数据库连接等.
 * <p>
 * 采集器 {@link CollectorRuntime} 用于实现对于一个通道的采集逻辑，负责对点位进行相关操作.如: 一个SQL查询语句，一个http请求.
 * <p>
 * 点位 {@link PointRuntime} 用于针对一个具体的数据点进行操作,如: 读取数据，写入数据等.
 *
 * @author zhouhao
 * @since 1.2.3
 */
public interface DataCollectorProvider extends CommandSupport {

    /**
     * 数据采集提供商标识
     *
     * @return 标识
     */
    String getId();

    /**
     * 数据采集提供商名称
     *
     * @return 名称
     */
    String getName();

    /**
     * 创建通道
     *
     * @param context 通道配置
     * @return 通道运行时
     */
    Mono<DataCollectorProvider.ChannelRuntime> createChannel(ChannelConfiguration context);

    /**
     * 重新加载通道
     *
     * @param runtime 通道运行时
     * @param context 通道配置
     * @return 通道运行时
     */
    Mono<DataCollectorProvider.ChannelRuntime> reloadChannel(DataCollectorProvider.ChannelRuntime runtime,
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

    /**
     * 数据采集器运行时，表示正在运行中的采集器.
     *
     * @author zhouhao
     * @see org.jetlinks.core.collector.discovery.DiscoveryPointCommand
     * @since 1.2.3
     */
    interface CollectorRuntime extends Lifecycle, CommandSupport {

        /**
         * 执行采集器的命令
         *
         * @param command 命令
         * @param <R>     结果类型
         * @return 执行结果
         */
        @Nonnull
        @Override
        <R> R execute(@Nonnull Command<R> command);

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
         * 订阅点位数据.
         * <p>
         * 用于接收通过{@link AccessMode#subscribe}采集到的数据.
         *
         * @return 点位数据
         * @see AccessMode#subscribe
         */
        Disposable subscribe(Consumer<PointData> listener);

        /**
         * 采集指定的点位数据. 用于主动获取点位数据.
         *
         * @param idList 点位ID
         * @return 点位数据
         * @see PointRuntime#read()
         * @see AccessMode#read
         */
        Flux<Result<PointData>> collect(Collection<String> idList);

    }

    interface PointRuntime extends Lifecycle {

        String getId();

        /**
         * 读取点位数据
         *
         * @return 点位数据
         */
        Mono<Result<PointData>> read();

        /**
         * 写入点位数据
         *
         * @param data 点位数据
         * @return 点位数据
         */
        Mono<Result<PointData>> write(PointData data);

    }

    interface Lifecycle {

        Mono<State> state();

        Mono<Void> start();

        Mono<Void> shutdown();

    }


    /**
     * 状态
     *
     * @author zhouhao
     * @see CollectorConstants.States
     * @since 1.2.3
     */
    interface State {

        String getValue();

        String getText();

    }
}
