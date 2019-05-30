package org.jetlinks.core.message.function;

import lombok.*;

import java.io.Serializable;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FunctionParameter implements Serializable {
    private static final long serialVersionUID = -6849794470754667710L;

    @NonNull
    private String name;

    private Object value;

    @Override
    public String toString() {
        return  name+"("+value+")";
    }
}
