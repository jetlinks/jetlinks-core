package org.jetlinks.core.lang;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class ReplacedSeparatedCharSequence extends AbstractSeparatedCharSequence {
    final AbstractSeparatedCharSequence source;
    final int i;
    final CharSequence r;

    @Override
    protected char separator() {
        return source.separator();
    }

    @Override
    protected int size0() {
        return source.size();
    }

    @Override
    protected CharSequence get0(int index) {
        if (index == this.i) {
            return r;
        }
        return source.get0(index);
    }

    @Override
    public SeparatedCharSequence replace(int index, CharSequence newChar) {
        if (index == i) {
            return new ReplacedSeparatedCharSequence(source, i, newChar);
        }
        return new ReplacedSeparatedCharSequence2(source, i, r, index, newChar);
    }
}
