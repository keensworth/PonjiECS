
package util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import org.joml.Random;
import org.joml.Vector2f;
import org.joml.sampling.Callback2d;

import javax.imageio.ImageIO;

public class PoissonGrid {
    public PoissonGrid() {
    }

    public static class Grid {
        private final Vector2f[] grid;
        private final float halfSize;
        private final float size;
        private float minDist;
        private float absMinDist;
        private float maxDist;
        private float minDistSquared;
        private float cellSize;
        private int numCells;
        private final Random rnd;
        private final ArrayList processList;
        private float[][] noise;

        public Grid(long seed, float halfSize, float minDist, int k, float[][] noise, Callback2d callback) {

            this.noise = noise;

            this.halfSize = halfSize;
            this.size = halfSize * 2;
            this.minDist = minDist;
            this.minDistSquared = minDist * minDist;
            this.maxDist = minDist * 5;
            this.absMinDist = 1f;
            this.rnd = new Random(seed);
            this.cellSize = minDist / (float)Math.sqrt(2.0D);
            this.numCells = (int)(halfSize * 2.0F / this.cellSize) + 1;
            this.grid = new Vector2f[this.numCells * this.numCells];
            this.processList = new ArrayList();
            this.compute(k, callback);
        }

        private void compute(int k, Callback2d callback) {
            float x;
            float y;
            do {
                x = this.rnd.nextFloat() * 2.0F - 1.0F;
                y = this.rnd.nextFloat() * 2.0F - 1.0F;
            } while(x * x + y * y > 1.0F);

            Vector2f initial = new Vector2f(x, y);
            this.processList.add(initial);
            callback.onNewSample(initial.x, initial.y);
            this.insert(initial);

            while(!this.processList.isEmpty()) {
                int i = this.rnd.nextInt(this.processList.size());
                Vector2f sample = (Vector2f)this.processList.get(i);
                boolean found = false;
                float elevation = (noise[(int)sample.y + (int)halfSize][(int)sample.x + (int)halfSize]+1)/2;
                this.minDist = absMinDist + ((elevation) * (maxDist - absMinDist));
                this.minDistSquared = minDist * minDist;

                for(int s = 0; s < k; ++s) {
                    float angle = this.rnd.nextFloat() * 6.2831855F;
                    float radius = this.minDist * (this.rnd.nextFloat() + 1.0F);
                    x = (float)((double)radius * Math.sin((double)angle + 1.5707963267948966D));
                    y = (float)((double)radius * Math.sin((double)angle));
                    x += sample.x;
                    y += sample.y;
                    if ((x>=-halfSize && x<=halfSize) && (y>=-halfSize && y<=halfSize) && !this.searchNeighbors(x, y)) {
                        found = true;
                        callback.onNewSample(x, y);
                        Vector2f f = new Vector2f(x, y);
                        this.processList.add(f);
                        this.insert(f);
                        break;
                    }
                }

                if (!found) {
                    this.processList.remove(i);
                }
            }

        }

        private boolean searchNeighbors(float px, float py) {
            int row = (int)((py + this.halfSize) / this.cellSize);
            int col = (int)((px + this.halfSize) / this.cellSize);
            if (this.grid[row * this.numCells + col] != null) {
                return true;
            } else {
                int minX = Math.max(0, col - 1);
                int minY = Math.max(0, row - 1);
                int maxX = Math.min(col + 1, this.numCells - 1);
                int maxY = Math.min(row + 1, this.numCells - 1);

                for(int y = minY; y <= maxY; ++y) {
                    for(int x = minX; x <= maxX; ++x) {
                        Vector2f v = this.grid[y * this.numCells + x];
                        if (v != null) {
                            float dx = v.x - px;
                            float dy = v.y - py;
                            if (dx * dx + dy * dy < this.minDistSquared) {
                                return true;
                            }
                        }
                    }
                }

                return false;
            }
        }

        private void insert(Vector2f p) {
            int row = (int)((p.y + this.halfSize) / this.cellSize);
            int col = (int)((p.x + this.halfSize) / this.cellSize);
            this.grid[row * this.numCells + col] = p;
        }
    }
}