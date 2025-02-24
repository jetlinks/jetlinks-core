package org.jetlinks.core.lang;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class ReplacedSeparatedCharSequence extends AbstractSeparatedCharSequence {
    final AbstractSeparatedCharSequence source;
    final int i;
    final CharSequence r;

    @Override
    public SeparatedCharSequence internInner() {
        source.internInner();
        if (r instanceof SeparatedCharSequence) {
            ((SeparatedCharSequence) r).internInner();
        }
        return this;
    }

    @Override
    public char separator() {
        return source.separator();
    }

    @Override
    public int size() {
        return source.size();
    }

    @Override
    public CharSequence get(int index) {
        if (index == this.i) {
            return r;
        }
        return source.get(index);
    }

    @Override
    public SeparatedCharSequence replace(int index, CharSequence newChar) {
        if (index == i) {
            return new ReplacedSeparatedCharSequence(source, i, newChar);
        }
        return new ReplacedSeparatedCharSequence2(source, i, r, index, newChar);
    }
}
