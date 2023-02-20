import javax.swing.tree.TreeNode;
import java.util.*;

public class MyHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
    // 默认数组大小 1 左移 4位 就是 16
    static final int DEFAULT_CAPACITY = 1 << 4;

    // 最大数组大小
    static final int MAX_CAPACITY = 1 << 30;

    // 默认负载因子
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    // 默认树化阈值
    static final int TREE_THRESHOLD = 8;

    // 默认逆树阈值
    static final int UN_TREE_THRESHOLD = 6;

    // 默认树化之前先检查元素个数是否大于64
    static final int MIN_TREE_CAPACITY = 64;

    // size 元素数
    int size;

    // 负载因子
    float loadFactor;

    // 数组扩容临界点
    int threshold;

    // 节点
    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        V value;
        Node<K, V> next;

        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return value;
        }

        public final String toString() {
            return key + "=" + value;
        }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                if (Objects.equals(key, e.getKey()) &&
                        Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
    }

    // Table
    Node<K, V>[] table;

    // hash产生地
    static final int hash(Object key) {
        int k = 0;
        // hash算法(底层 &与 数组长度（这就是为什么数组长度必须为2的倍数 以为奇数&永远是1，这样只有奇数桶可以被存储）) 和 扰动函数
        // key 值可以为空
        return (key == null) ? 0 : (k = key.hashCode()) ^ (k >>> 16);
    }

    // 构造方法
    public MyHashMap(int initCapacity, float loadFactor) {
        // 判断 initCapacity 是否合规
        if (initCapacity < 0)
            throw new IllegalArgumentException("数组长度定义不合规" + initCapacity);
        if (initCapacity > MAX_CAPACITY)
            initCapacity = MAX_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("负载因子不合规: " +
                    loadFactor);

        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initCapacity);
    }

    public MyHashMap(int initCapacity) {
        this(initCapacity, DEFAULT_LOAD_FACTOR);
    }

    public MyHashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
    }

    // 将输入的数字转为2的平方 通过不断地右移。将第一位的奇数位抹为0
    public int tableSizeFor(int capacity) {
        int cut = capacity - 1;
        cut |= cut >>> 1;
        cut |= cut >>> 2;
        cut |= cut >>> 4;
        cut |= cut >>> 8;
        cut |= cut >>> 16;
        return (cut < 0) ? 1 : (cut > MAX_CAPACITY) ? MAX_CAPACITY : cut + 1;
    }

    // Put 方法
    public V put(K key, V value) {
        return putVal(hash(key), key, value);
    }

    // putVal
    public V putVal(int hash, K key, V value) {
        Node<K, V>[] tab;
        int l, i;
        Node<K, V> p, f = null;
        K k;
        // 数组初始化
        if ((tab = table) == null || (l = table.length) == 0) {
            l = (tab = resize()).length;
        }
        // 判断数组桶位第一个元素是否为空
        if ((p = tab[i = (l - 1) & hash]) == null) {
            table[i] = new Node<>(hash, key, value, null);
        }
        // 如果元素不为空往后遍历桶位元素
        else {
            // 查看当前桶位第一个元素key值是否一样 hash是否一样 如果一样则执行替换而不是插入
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k)))) {
                // 标志位
                f = p;
            }
            // 判断是否为红黑树
            else if (p instanceof TreeNode) {
                // 执行红黑树插入逻辑
            }
            // 不为红黑树那就执行数组逻辑，遍历桶位里的元素。
            else if (p == null) {
                // 桶位元素遍历数
                int binCount = 0;
                while (true) {
                    // 如果为空直接插入
                    if ((f = p.next) == null) {
                        p.next = new Node<>(hash, key, value, null);
                        break;
                    } else if (binCount == TREE_THRESHOLD - 1) {
                        // TODO：执行红黑树化擦操作。 这里注意如果小与64还是会扩展数组而不数化
                        break;
                    }
                    // 查看当前桶位当前元素key值是否一样 hash是否一样 如果一样则执行替换而不是插入
                    else if (f.hash == hash && ((k = f.key) == key || (key != null && key.equals(k)))) {
                        // 标志位
                        f = p;
                        break;
                    }
                    binCount++;
                }
                p = f;
            }

        }
        // 如果标志位f不为空说明 key的值是一样的只需要替换数值并且没有进行插入操作
        if (f != null) {
            V oldValue = f.value;
            f.value = value;
            return oldValue;
        }

        // 其实基本不会进行树化，因为一点插入元素到了临界值就会通过resize增大数组容量，而不是树化。 如果只是修改是不会执行++size这一步的
        if (++size > threshold) {
            resize();
        }
        return null;
    }

    // resize
    public Node<K, V>[] resize() {
        Node<K, V>[] oldTab = table;
        int oldCpa = (oldTab == null) ? 0 : table.length;
        int oldThr = threshold;
        int newCpa = 0;
        int newThr = 0;
        // 用于数组扩容
        if (oldCpa > 0) {
            if (oldCpa > MAX_CAPACITY) {
                this.threshold = Integer.MAX_VALUE;
            }
            if ((newCpa = oldCpa << 1) < MAX_CAPACITY && oldCpa >= DEFAULT_CAPACITY) {
                newThr = oldThr << 1;
            }
        }
        // 用于初始化数组
        else if (threshold > 0) {
            newCpa = threshold;
        } else {
            newCpa = DEFAULT_CAPACITY;
            newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_LOAD_FACTOR);
        }

        if (newThr == 0) {
            float ft = (float) newCpa * loadFactor;
            newThr = (newCpa < MAX_CAPACITY && ft < (float) MAX_CAPACITY ?
                    (int) ft : Integer.MAX_VALUE);
        }
        // 创建新数组
        Node<K, V>[] newTable = (Node<K, V>[]) new Node[newCpa];
        // 赋值负载因子
        this.threshold = newThr;
        // 赋值table
        this.table = newTable;

        // 新老数组更替逻辑   高位和低位  数组扩容后 &的二进制范围扩大 一些大于16的二进制位可以被获取到 所有要改变
        if (oldTab != null) {
            Node<K, V> e;
            for (int i = 0; i < oldCpa; i++) {
                if ((e = oldTab[i]) != null) {
                    oldTab[i] = null;
                    // 如果只有一个元素
                    if (e.next == null) {
                        newTable[(e.hash & (newCpa - 1))] = e;
                    }
                    // 有很多元素 如果为红黑树
                    else if (e instanceof TreeNode) {
                        // TODO：执行红黑树逻辑
                    }
                    // 如果为数组
                    else {
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            next = e.next;
                            // 判断是否为低位
                            if ((e.hash & oldCpa) == 0) {
                                if (loTail == null) {
                                    loHead = e;
                                }else {
                                    loTail.next = e;
                                }
                                loTail = e;
                            } else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        }while ((e = next) != null);
                        // 低位赋值
                        if (loTail != null) {
                            loTail.next = null;
                            newTable[i] = loHead;
                        }
                        // 高位赋值
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTable[i + oldCpa] = hiHead;
                        }
                    }
                }
            }
        }
        return newTable;
    }

    public V get(Object key) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (first = tab[(n - 1) & hash]) != null) {
            if (first.hash == hash && // always check first node
                    ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            if ((e = first.next) != null) {
                if (first instanceof TreeNode)
                    // TODO： 红黑树操作
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }

    public boolean containsKey(Object key) {
        return getNode(hash(key), key) != null;
    }

    public boolean containsValue(Object value) {
        Node<K,V>[] tab; V v;
        if ((tab = table) != null && size > 0) {
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    if ((v = e.value) == value ||
                            (value != null && value.equals(v)))
                        return true;
                }
            }
        }
        return false;
    }
    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }

}
