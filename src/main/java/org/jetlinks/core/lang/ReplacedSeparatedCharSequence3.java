package org.jetlinks.core.lang;

class ReplacedSeparatedCharSequence3 extends ReplacedSeparatedCharSequence2 {

    final int i3;
    final CharSequence r3;

    public ReplacedSeparatedCharSequence3(AbstractSeparatedCharSequence source,
                                          int i, CharSequence r,
                                          int i2, CharSequence r2,
                                          int i3, CharSequence r3) {
        super(source, i, r, i2, r2);
        this.i3 = i3;
        this.r3 = r3;
    }

    @Override
    public SeparatedCharSequence internInner() {
        super.internInner();
        if (r3 instanceof SeparatedCharSequence) {
            ((SeparatedCharSequence) r3).internInner();
        }
        return this;
    }

    @Override
    public CharSequence get(int index) {
        if (index == this.i3) {
            return r3;
        }
        return super.get(index);
    }

    @Override
    public SeparatedCharSequence replace(int index, CharSequence newChar) {
        if (index == i) {
            return new ReplacedSeparatedCharSequence3(source, i, newChar, i2, r2, i3, r3);
        }
        if (index == i2) {
            return new ReplacedSeparatedCharSequence3(source, i, r, i2, newChar, i3, r3);
        }
        if (index == i3) {
            return new ReplacedSeparatedCharSequence3(source, i, r, i2, r2, i3, newChar);
        }
        return new ReplacedSeparatedCharSequence(this, index, newChar);
    }
}
