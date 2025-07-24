package org.jetlinks.core.collector;

import org.jetlinks.core.Wrapper;
import org.jetlinks.core.collector.command.GetChannelConfigMetadataCommand;
import org.jetlinks.core.collector.command.GetCollectorConfigMetadataCommand;
import org.jetlinks.core.collector.subscribe.PointSubscriber;
import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.metadata.Feature;
import org.jetlinks.core.monitor.Monitor;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 针对数据采集的支持,用于平台主动采集的场景,如: 定时采集modbus数据等.
 * <p>
 * 数采集由通道 、采集器 、点位组成.
 * <p>
 * 通道 {@link ChannelRuntime} 用于实现一个具体的数据通道，如: 一个modbus主站，一个TCP客户端，一个数据库连接等.
 * <p>
 * 采集器 {@link CollectorRuntime} 用于实现对于一个通道的采集逻辑，负责对点位进行相关操作.如: 一个SQL查询语句，一个http请求.
 * <p>
 * 点位 {@link PointRuntime} 用于针对一个具体的数据点进行操作,如: 读取数据，写入数据等.
 *
 * @author zhouhao
 * @since 1.2.3
 * @see GetChannelConfigMetadataCommand
 * @see GetCollectorConfigMetadataCommand
 * @see org.jetlinks.core.collector.command.GetPointConfigMetadataCommand
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
     * @param configuration 通道配置
     * @return 通道运行时
     */
    Mono<DataCollectorProvider.ChannelRuntime> createChannel(ChannelConfiguration configuration);

    /**
     * 创建采集器
     *
     * @param configuration 采集器配置
     * @return 采集器运行时
     */
    Mono<DataCollectorProvider.CollectorRuntime> createCollector(CollectorConfiguration configuration);

    /**
     * 创建点位运行时
     *
     * @param configuration 点位配置
     * @return 点位运行时
     */
    Mono<DataCollectorProvider.PointRuntime> createPoint(PointConfiguration configuration);

    interface ChannelConfiguration {

        /**
         * 获取通道配置
         *
         * @return 通道配置
         */
        ChannelProperties getProperties();

        /**
         * 监控器
         *
         * @return 监控器
         * @see Monitor#logger()
         * @see Monitor#tracer()
         */
        Monitor monitor();

    }

    interface CollectorConfiguration {

        /**
         * 获取通道配置
         *
         * @return 通道配置
         */
        CollectorProperties getProperties();

        /**
         * 创建通道监控器
         *
         * @return 监控器
         */
        Monitor monitor();

        /**
         * 通道运行时
         *
         * @return 通道运行时
         */
        DataCollectorProvider.ChannelRuntime channel();
    }

    interface PointConfiguration {
        /**
         * 获取通道配置
         *
         * @return 通道配置
         */
        PointProperties getProperties();

        /**
         * 监控器
         *
         * @return 监控器
         * @see Monitor#logger()
         * @see Monitor#tracer()
         */
        Monitor monitor();

        /**
         * 获取通道运行时
         *
         * @return ChannelRuntime
         */
        ChannelRuntime channel();

        /**
         * 获取采集器运行时
         *
         * @return CollectorRuntime
         */
        CollectorRuntime collector();
    }

    /**
     * 通道运行时,用于执行通信等操作.
     *
     * @author zhouhao
     * @since 1.2.3
     */
    interface ChannelRuntime extends Lifecycle, CommandSupport {

        /**
         * 获取通道ID
         *
         * @return 通道ID
         */
        String getId();

    }

    /**
     * 数据采集器运行时，用于执行采集逻辑.
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
         * 订阅点位数据.如果不支持则返回{@link Disposables#disposed()}.
         * <p>
         * 调用返回值{@link Disposable#dispose()}取消订阅.
         * <p>
         * 当点位产生数据时,调用监听器{@link Consumer#accept(Object)}方法.
         *
         * @return Disposable
         * @see AccessMode#subscribe
         */
        Disposable subscribe(PointRuntime point,
                             PointSubscriber subscriber);

        /**
         * 采集指定的点位数据. 用于主动获取点位数据，如定时获取等.
         *
         * @return 点位数据
         * @see PointRuntime#read()
         * @see AccessMode#read
         */
        Flux<Result<PointData>> collect(Collection<? extends PointRuntime> points);

        /**
         * 获取采集器支持的特性
         *
         * @return 特性
         * @see CollectorConstants.CollectorFeatures
         */
        Set<? extends Feature> getFeatures();

        /**
         * 判断是否支持特性
         *
         * @param feature 特性
         * @return 支持：true，不支持：false。
         */
        default boolean hasFeature(Feature feature) {
            return getFeatures().contains(feature);
        }
    }

    /**
     * 点位运行时
     *
     * @since 1.2.3
     */
    interface PointRuntime extends CommandSupport, Lifecycle {

        /**
         * 点位ID
         *
         * @return 点位ID
         */
        String getId();

        /**
         * 测试点位,返回点位健康度.
         *
         * @return 测试结果
         * @see Result#getCode()
         */
        Mono<Result<Health>> test();

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

    /**
     * 生命周期,用于管理状态等逻辑.
     *
     * @since 1.2.3
     */
    interface Lifecycle extends Wrapper, Disposable {

        /**
         * 检查状态
         *
         * @return 检查状态
         */
        Mono<State> checkState();

        /**
         * 当前状态
         * @return 状态
         */
        State state();

        /**
         * 启动
         */
        void start();

        /**
         * 暂停
         */
        void pause();

        /**
         * 停止
         */
        void dispose();

        /**
         * 监听状态变化
         *
         * @param listener 状态变化
         * @return Disposable
         */
        Disposable onStateChanged(BiConsumer<State, State> listener);
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
