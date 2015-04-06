package kontraktor;

import org.junit.Test;

import java.util.Random;

/**
 * Created by ruedi on 06/04/15.
 */
public class StupidInternetBench {

    	public static class Vec2 {
		public Vec2(double x, double y) {
			this.x = x;
			this.y = y;
		}

		public double x;
		public double y;
	}

	private static class Noise2DContext {
		Vec2[] rgradients;
		int[] permutations;
		Vec2[] gradients;
		Vec2[] origins;

		private static final double lerp(double a, double b, double v) {
			return a * (1 - v) + b * v;
		}

		private static final double smooth(double v) {
			return v * v * (3 - 2 * v);
		}

		Vec2 random_gradient(Random rnd) {
			double v = rnd.nextDouble() * Math.PI * 2.0;
			return new Vec2((double) Math.cos(v), (double) Math.sin(v));
		}

		double gradient(Vec2 orig, Vec2 grad, Vec2 p) {
			double xx = p.x - orig.x;
            double yy = p.y - orig.y;
			return grad.x * xx + grad.y * yy;
		}

		public Noise2DContext(int seed) {
			Random rnd = new Random(seed);
			rgradients = new Vec2[256];
			permutations = new int[256];
			for (int i = 0; i < 256; i++) {
				rgradients[i] = random_gradient(rnd);
			}
			for (int i = 0; i < 256; i++) {
				int j = rnd.nextInt(i + 1);
				permutations[i] = permutations[j];
				permutations[j] = i;
			}

			gradients = new Vec2[4];
			origins = new Vec2[4];
			origins[0] = new Vec2(0, 0);
			origins[1] = new Vec2(0, 0);
			origins[2] = new Vec2(0, 0);
			origins[3] = new Vec2(0, 0);
		}

		Vec2 get_gradient(int x, int y) {
			int idx = permutations[x & 255] + permutations[y & 255];
			return rgradients[idx & 255];
		}

		void get_gradients(double x, double y) {
			double x0f = Math.floor(x);
			double y0f = Math.floor(y);
			int x0 = (int) x0f;
			int y0 = (int) y0f;
			int x1 = x0 + 1;
			int y1 = y0 + 1;

			gradients[0] = get_gradient(x0, y0);
			gradients[1] = get_gradient(x1, y0);
			gradients[2] = get_gradient(x0, y1);
			gradients[3] = get_gradient(x1, y1);

			origins[0].x = x0f + 0; origins[0].y = y0f + 0;
			origins[1].x = x0f + 1; origins[1].y = y0f + 0;
			origins[2].x = x0f + 0; origins[2].y = y0f + 1;
			origins[3].x = x0f + 1; origins[3].y = y0f + 1;
		}

	    Vec2 p = new Vec2(0, 0);
		public double get(double x, double y) {
            p.x = x; p.y = y;
			get_gradients(x, y);
			double v0 = gradient(origins[0], gradients[0], p);
			double v1 = gradient(origins[1], gradients[1], p);
			double v2 = gradient(origins[2], gradients[2], p);
			double v3 = gradient(origins[3], gradients[3], p);

			double fx = smooth(x - origins[0].x);
			double vx0 = lerp(v0, v1, fx);
			double vx1 = lerp(v2, v3, fx);
			double fy = smooth(y - origins[0].y);
			return lerp(vx0, vx1, fy);
		}
	}

	static char[] symbols = { ' ', '░', '▒', '▓', '█', '█' };

	public StupidInternetBench() {
		Noise2DContext n2d = new Noise2DContext((int) System.currentTimeMillis());
		double[] pixels = new double[256 * 256];

		for (int i = 0; i < 100; i++) {
			for (int y = 0; y < 256; y++) {
				for (int x = 0; x < 256; x++) {
					double v = n2d.get(x * 0.1f, y * 0.1f) * 0.5f + 0.5f;
					pixels[y * 256 + x] = v;
				}
			}
		}

//		for (int y = 0; y < 256; y++) {
//			for (int x = 0; x < 256; x++) {
//				int idx = (int) (pixels[y * 256 + x] / 0.2f);
//				System.out.print(symbols[idx]);
//			}
//			System.out.println();
//		}
	}

	public static void main(String[] args) {
        while( true ) {
            long now = System.currentTimeMillis();
            new StupidInternetBench();
            System.out.println("time:"+(System.currentTimeMillis()-now));
        }
    }

}
