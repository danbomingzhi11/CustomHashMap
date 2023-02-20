import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        int n = 14;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        System.out.println(n + 1);
        MyHashMap hashMap = new MyHashMap<>();
        hashMap.put(123, "ffff");
        System.out.println(hashMap.get(123));

    }
}
