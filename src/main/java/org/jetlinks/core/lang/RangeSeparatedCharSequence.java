package org.jetlinks.core.lang;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class RangeSeparatedCharSequence extends AbstractSeparatedCharSequence {
    private final AbstractSeparatedCharSequence source;

    private final int from, size;

    @Override
    public char separator() {
        return source.separator();
    }

    @Override
    protected int size0() {
        return size;
    }

    @Override
    protected CharSequence get0(int index) {

        return source.get(index + from);
    }
}
