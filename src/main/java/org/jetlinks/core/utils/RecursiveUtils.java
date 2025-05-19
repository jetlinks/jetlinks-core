package org.jetlinks.core.utils;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.exception.RecursiveCallException;
import reactor.util.context.Context;

import java.util.function.Function;

public class RecursiveUtils {

    public static Function<Context, Context> validator(String operation, int maxRecursive) {
        return new Validator(operation, maxRecursive);
    }

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode(of = "operation")
    public static class Validator implements Function<Context, Context> {
        private final String operation;
        private final int maxRecursive;

        @Override
        public Context apply(Context context) {
            int num = context.<Integer>getOrEmpty(this).orElse(0);
            if (num > maxRecursive) {
                throw new RecursiveCallException(operation,maxRecursive);
            }
            return context.put(this, num + 1);
        }
    }
}
