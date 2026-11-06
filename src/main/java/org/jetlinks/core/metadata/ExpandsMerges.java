package org.jetlinks.core.metadata;

import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * 类加载器内统一的扩展合并策略入口，不解释具体扩展的含义。
 * SPI 提供默认规则，应用注册可覆盖同名顶层键；注销恢复其余有效注册或 SPI 默认值。
 * 更新时发布不可变快照，读取无锁，已取得的策略与已构造的元数据不受后续注册影响。
 *
 * @since 1.3.3
 * @see ExpandsMergeProvider
 */
public final class ExpandsMerges {
    private static final Map<String, ExpandsMerge> defaults = loadDefaults();
    private static final List<Registration> registrations = new ArrayList<>();
    private static volatile ExpandsMerge strategy = ExpandsMerge.object(defaults);

    private ExpandsMerges() {
    }

    /**
     * 取得本次解析或合并使用的不可变快照，不逐字段重新查找规则。
     * 未注册字段继续使用调用入口提供的原有覆盖策略。
     *
     * @return 当前默认合并策略
     */
    public static ExpandsMerge defaultStrategy() {
        return strategy;
    }

    /**
     * 注册单个顶层扩展键的完整规则；后注册覆盖先注册，返回句柄由注册方释放。
     *
     * @param key 扩展键
     * @param rule 不可变规则，对象规则的子键不会与被覆盖的规则隐式拼接
     * @return 可重复注销的句柄，仅移除本次注册
     */
    public static Disposable register(String key, ExpandsMerge rule) {
        return register(Map.of(key, rule));
    }

    /**
     * 原子注册一组规则。复制输入并发布完整快照；只影响之后取得的默认策略，
     * 不追溯刷新元数据或改变显式传入的局部规则。注册及注销可并发调用。
     *
     * @param rules 顶层键与完整规则，不可含 null
     * @return 由注册方保管的可重复注销句柄；乱序注销不会覆盖其他有效注册
     */
    public static synchronized Disposable register(Map<String, ExpandsMerge> rules) {
        Registration registration = new Registration(Map.copyOf(rules));
        registrations.add(registration);
        refresh();
        return registration;
    }

    private static Map<String, ExpandsMerge> loadDefaults() {
        Map<String, ExpandsMerge> rules = new LinkedHashMap<>();
        for (ExpandsMergeProvider provider : ServiceLoader.load(ExpandsMergeProvider.class,
                                                               ExpandsMergeProvider.class.getClassLoader())) {
            provider.getRules().forEach((key, rule) -> {
                // SPI 枚举顺序不是覆盖优先级；重叠定义须改为有生命周期的显式注册。
                if (rules.putIfAbsent(key, rule) != null) {
                    throw new IllegalStateException("Duplicate expands merge rule: " + key);
                }
            });
        }
        return Map.copyOf(rules);
    }

    private static void refresh() {
        Map<String, ExpandsMerge> rules = new LinkedHashMap<>(defaults);
        registrations.forEach(registration -> rules.putAll(registration.rules));
        strategy = ExpandsMerge.object(rules);
    }

    /** 身份相同的规则可多次注册，注销按句柄身份处理。 */
    private static final class Registration implements Disposable {
        private final Map<String, ExpandsMerge> rules;
        private volatile boolean disposed;

        private Registration(Map<String, ExpandsMerge> rules) {
            this.rules = rules;
        }

        @Override
        public void dispose() {
            synchronized (ExpandsMerges.class) {
                if (registrations.remove(this)) {
                    refresh();
                    disposed = true;
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
