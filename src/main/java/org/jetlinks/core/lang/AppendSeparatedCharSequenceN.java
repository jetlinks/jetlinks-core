package org.jetlinks.core.lang;

class AppendSeparatedCharSequenceN extends AbstractSeparatedCharSequence {

    private final AbstractSeparatedCharSequence source;
    private final CharSequence[] appendN;

    AppendSeparatedCharSequenceN(AbstractSeparatedCharSequence source,
                                 CharSequence[] appendN) {
        this.source = source;
        this.appendN = appendN;
    }

    @Override
    protected char separator() {
        return source.separator();
    }

    @Override
    protected int size0() {
        return source.size() + appendN.length;
    }

    @Override
    protected CharSequence get0(int index) {
        int size = source.size();

        if (index >= size) {
            return appendN[index - size];
        }

        return source.get(index);
    }
}
