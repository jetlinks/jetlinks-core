package org.jetlinks.core.lang;


class AppendSeparatedCharSequence extends AbstractSeparatedCharSequence {

    final AbstractSeparatedCharSequence source;
    final CharSequence append;

    AppendSeparatedCharSequence(AbstractSeparatedCharSequence source,
                                CharSequence append) {
        this.source = source;
        this.append = append;
    }

    @Override
    public SeparatedCharSequence internInner() {
        source.internInner();
        return this;
    }

    @Override
    public char separator() {
        return source.separator();
    }

    @Override
    protected int size0() {
        return source.size() + 1;
    }

    @Override
    protected CharSequence get0(int index) {
        if (index == source.size()) {
            return append;
        }
        return source.get(index);
    }

}
