package language.utils;

public class Pair<First, Last> {
    private First first;
    private Last last;

    public Pair(First first, Last last) {
        this.first = first;
        this.last = last;
    }

    public Pair() {}

    @Override
    public String toString() {
        return "Pair{" +
                "first=" + first +
                ", last=" + last +
                '}';
    }

    public First getFirst() {
        return first;
    }

    public void setFirst(First first) {
        this.first = first;
    }

    public Last getLast() {
        return last;
    }

    public void setLast(Last last) {
        this.last = last;
    }
}
