package org.jetlinks.core.metadata;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.collections.MapUtils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 合并选项，通过一些自定义的选项来指定合并过程中的行为,比如 忽略合并一下拓展配置等
 *
 * @author zhouhao
 * @since 1.1.6
 */
public interface MergeOption {
    MergeOption ignoreExists = DefaultMergeOption.ignoreExists;
    MergeOption overwriteProperty = DefaultMergeOption.overwriteProperty;

    MergeOption[] DEFAULT_OPTIONS = new MergeOption[0];

    static PropertyFilter propertyFilter(PropertyFilter filter) {
        return filter;
    }

    String getId();

    enum DefaultMergeOption implements MergeOption {
        ignoreExists,
        mergeExpands,
        overwriteProperty;

        @Override
        public String getId() {
            return name();
        }

    }

    interface PropertyFilter extends MergeOption, Predicate<PropertyMetadata> {
        @Override
        default String getId() {
            return "PropertyFilter";
        }

        /**
         * 属性过滤校验。返回true表示合并此属性。false表示不合并
         *
         * @param metadata 属性物模型
         * @return 是否合并
         */
        boolean test(PropertyMetadata metadata);

        static boolean doFilter(PropertyMetadata metadata, MergeOption... option) {
            for (MergeOption mergeOption : option) {
                if (mergeOption instanceof PropertyFilter) {
                    if (!((PropertyFilter) mergeOption).test(metadata)) {
                        return false;
                    }
                }
            }
            return true;
        }

        static boolean has(MergeOption[] options) {
            for (MergeOption option : options) {
                if (option instanceof PropertyFilter) {
                    return true;
                }
            }
            return false;
        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ExpandsMerge implements MergeOption {
        private static final ExpandsMerge all = new ExpandsMerge(null, Type.ignore) {
            @Override
            public boolean isIgnore(String key) {
                return true;
            }

            @Override
            public void mergeExpands(Map<String, Object> from, BiConsumer<String, Object> to) {

            }
        };
        private final Set<String> keys;
        private final Type type;

        public static ExpandsMerge ignore(String... keys) {
            return new ExpandsMerge(Arrays.stream(keys).collect(Collectors.toSet()), Type.ignore);
        }

        public static ExpandsMerge remove(String... keys) {
            return new ExpandsMerge(Arrays.stream(keys).collect(Collectors.toSet()), Type.remove);
        }

        public ExpandsMerge ignoreAll() {
            return all;
        }

        @Override
        public String getId() {
            return "expandsMerge";
        }

        public boolean isIgnore(String key) {
            return keys.contains(key);
        }

        public static Optional<ExpandsMerge> from(MergeOption option) {
            if (option instanceof ExpandsMerge) {
                return Optional.of(((ExpandsMerge) option));
            }
            return Optional.empty();
        }

        public void mergeExpands(Map<String, Object> from, BiConsumer<String, Object> to) {
            from.forEach((key, value) -> {
                if (!isIgnore(key)) {
                    to.accept(key, value);
                }
            });
        }

        public static void doWith(DeviceMetadataType metadataType,
                                  Map<String, Object> from,
                                  Map<String, Object> to,
                                  MergeOption... options) {
            if (MapUtils.isEmpty(from)) {
                return;
            }
            boolean merged = false;

            if (options != null && options.length != 0) {
                for (MergeOption option : options) {
                    ExpandsMerge expandsOption = ExpandsMerge.from(option).orElse(null);
                    if (null != expandsOption) {
                        merged = true;
                        expandsOption.mergeExpands(from, to::put);
                        if (expandsOption.type == Type.remove) {
                            expandsOption.keys.forEach(to::remove);
                        }
                    }
                }
            }

            if (!merged) {
                from.forEach(to::put);
            }
        }

        @AllArgsConstructor
        private enum Type {
            ignore,
            remove;
        }
    }

    static boolean has(MergeOption option, MergeOption... target) {

        for (MergeOption mergeOption : target) {
            if (Objects.equals(option, mergeOption)) {
                return true;
            }
        }
        return false;
    }

}
