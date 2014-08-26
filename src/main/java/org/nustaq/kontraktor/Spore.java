package org.nustaq.kontraktor;

/**
 * Created by ruedi on 26.08.2014.
 * <pre>
 * int x = 100;
 * int y = 111;
 * String s = "pok";
 *
 * public Spore<Integer,String> getSpore(int z) {
 *     return new Spore<Integer,String>() {
 *         // declare
 *         int sx,sy,sz;
 *         HashMap map;
 *
 *         {
 *             // capture
 *             sx = x; sy = y; sz = z;
 *             map = new HashMap();
 *         }
 *
 *         public void body(Integer in, Callback<String> out) {
 *             System.out.println("executed later " + sx + " " + sy + " " + sz);
 *         }
 *     };
 * }
 * </pre>
 */
public interface Spore<I,O> {
    public void body( I input, Callback<O> output );
}
