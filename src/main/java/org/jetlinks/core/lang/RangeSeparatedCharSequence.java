package org.jetlinks.core.lang;

class RangeSeparatedCharSequence extends AbstractSeparatedCharSequence {
    private final AbstractSeparatedCharSequence source;

    private final int from, end;

    public RangeSeparatedCharSequence(AbstractSeparatedCharSequence source, int from, int end) {
        this.source = source;
        if(end > source.size()){
            throw new StringIndexOutOfBoundsException(end);
        }
        this.from = from;
        this.end = end;
    }

    @Override
    public char separator() {
        return source.separator();
    }

    @Override
    public int size() {
        return end - from;
    }

    @Override
    public CharSequence get(int index) {

        return source.get(index + from);
    }
}
