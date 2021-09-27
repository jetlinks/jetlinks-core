package org.jetlinks.core.metadata.unit;

import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 单位统一管理工具
 *
 * @author zhouhao
 * @since 1.1
 */
@Slf4j
public class ValueUnits {

    private static final List<ValueUnitSupplier> suppliers = new CopyOnWriteArrayList<>();

    static {
        ValueUnits.register(new ValueUnitSupplier() {
            @Override
            public Optional<ValueUnit> getById(String id) {
                return Optional.ofNullable(UnifyUnit.of(id));
            }

            @Override
            public List<ValueUnit> getAll() {
                return Arrays.asList(UnifyUnit.values());
            }
        });
    }

    /**
     * 注册一个自定义单位提供商,可调用返回值{@link Disposable#dispose()}进行注销
     *
     * @param supplier 提供商
     * @return Disposable
     */
    public static Disposable register(ValueUnitSupplier supplier) {
        suppliers.add(supplier);
        return () -> suppliers.remove(supplier);
    }

    /**
     * 通过单位ID来获取单位信息,如果id不存在则将id当成符号作为单位,如果id是json格式,则解析json为单位
     *
     * @param id ID
     * @return 单位
     */
    public static Optional<ValueUnit> lookup(String id) {
        for (ValueUnitSupplier supplier : suppliers) {
            try {
                Optional<ValueUnit> unit = supplier.getById(id);
                if (unit.isPresent()) {
                    return unit;
                }
            }catch (Error ignore){
                suppliers.remove(supplier);
            }
        }
        //json ?
        if (id.startsWith("{")) {
            return Optional.ofNullable(JsonValueUnit.of(id));
        }
        return Optional.of(SymbolValueUnit.of(id));
    }

    /**
     * 获取全部单位
     *
     * @return 单位列表
     */
    public static List<ValueUnit> getAllUnit() {
        return suppliers
                .stream()
                .flatMap(supplier -> {
                    try {
                        return supplier.getAll().stream();
                    } catch (Error err) {
                        //由协议包自定义的单位,当协议被卸载时可能会报错
                        suppliers.remove(supplier);
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toList());
    }
}
