package org.jetlinks.core.utils;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.exception.RecursiveCallException;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.function.Function;

public class RecursiveUtils {

    public static Function<Context, Context> validator(String operation, int maxRecursive) {
        return new Validator(operation, maxRecursive);
    }

    public static boolean hasRecursive(ContextView ctx, String operation, int maxRecursive) {
        if (maxRecursive >= 0 && ctx.hasKey(Validator.class)) {
            Integer num = ctx
                .getOrDefault(new Validator(operation, maxRecursive), 0);
            return num != null && num > maxRecursive;
        }
        return false;
    }

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode(of = "operation")
    public static class Validator implements Function<Context, Context> {
        private final String operation;
        private final int maxRecursive;

        @Override
        public Context apply(Context context) {
            Integer num = context.getOrDefault(this, 0);
            if (num != null && num > maxRecursive) {
                throw new RecursiveCallException(operation, maxRecursive);
            }
            return context
                .put(this, num == null ? 1 : num + 1)
                .put(Validator.class, true);
        }
    }
}
