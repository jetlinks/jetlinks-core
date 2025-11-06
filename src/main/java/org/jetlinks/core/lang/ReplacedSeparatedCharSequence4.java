package org.jetlinks.core.lang;

class ReplacedSeparatedCharSequence4 extends ReplacedSeparatedCharSequence3 {

    final int i4;
    final CharSequence r4;

    public ReplacedSeparatedCharSequence4(AbstractSeparatedCharSequence source,
                                          int i, CharSequence r,
                                          int i2, CharSequence r2,
                                          int i3, CharSequence r3,
                                          int i4, CharSequence r4) {
        super(source, i, r, i2, r2, i3, r3);
        this.i4 = i4;
        this.r4 = r4;
    }

    @Override
    public SeparatedCharSequence internInner() {
        super.internInner();
        if (r4 instanceof SeparatedCharSequence) {
            ((SeparatedCharSequence) r4).internInner();
        }
        return this;
    }

    @Override
    public CharSequence get(int index) {
        if (index == this.i4) {
            return r4;
        }
        return super.get(index);
    }

    @Override
    public SeparatedCharSequence replace(int index, CharSequence newChar) {
        if (index == i) {
            return new ReplacedSeparatedCharSequence4(source, i, newChar, i2, r2, i3, r3, i4, r4);
        }
        if (index == i2) {
            return new ReplacedSeparatedCharSequence4(source, i, r, i2, newChar, i3, r3, i4, r4);
        }
        if (index == i3) {
            return new ReplacedSeparatedCharSequence4(source, i, r, i2, r2, i3, newChar, i4, r4);
        }
        if (index == i4) {
            return new ReplacedSeparatedCharSequence4(source, i, r, i2, r2, i3, r3, i4, newChar);
        }
        return new ReplacedSeparatedCharSequence(this, index, newChar);
    }
}
