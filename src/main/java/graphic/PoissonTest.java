package graphic;

public class PoissonTest {
    private static final int WIDTH = 128;
    private static final int HEIGHT = 128;
    public static void main(String[] args) {
        test();
        /*
        for (int j = 0; j < 5; j++) {
            long start = System.nanoTime();
            final int[] count = {0};
            PoissonSampling.Disk disk = new PoissonSampling.Disk(1, 16 * 1.71f, 8, 4, new Callback2d() {
                @Override
                public void onNewSample(float v, float v1) {
                    count[0]++;
                }
            });

            long end = System.nanoTime();
            System.out.println((end - start) / 1000000f + " ms | " + count[0]);
        }

        long start1 = System.nanoTime();
        int[] fill = new int[1000];
        for (int j = 0; j < 1000; j++) {
            fill[j] = j*j*j*j;
        }
        long end1 = System.nanoTime();
        System.out.println((end1 - start1) / 1000000f + " ms");
        for (int j = 0; j < 5; j++) {
            long start = System.nanoTime();
            final int[] count = {0};
            PoissonSampling.Disk disk = new PoissonSampling.Disk(1, 16 * 1.71f, 8, 4, new Callback2d() {
                @Override
                public void onNewSample(float v, float v1) {
                    count[0]++;
                }
            });

            long end = System.nanoTime();
            System.out.println((end - start) / 1000000f + " ms | " + count[0]);
        }

         */
        /*
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++){
            for (int x = 0; x < image.getWidth(); x++){
                image.setRGB(x,y,0xFFFFFF);
            }
        }
        /*
        PoissonGrid.Grid disk = new PoissonGrid.Grid(1, HEIGHT/2, 6, 30, new Callback2d() {
            @Override
            public void onNewSample(float v, float v1) {
                image.setRGB((int)v+WIDTH/2,(int)v1+HEIGHT/2,0x000000);
            }
        });


        image.setRGB(10,10,0x010000*255);
        image.setRGB(10,10,0x010000*255);
        try {
            ImageIO.write(image, "png", new File("poisson_out.png"));
        } catch (Exception e){
            System.out.println("you messed up man");
        }
        */
    }

    private static boolean objectBelow(int x, int y, int[][] presenceArray, int distance){
        for (int j = y; j > y-distance; j--){
            for (int i = x+distance; i > x-distance; i--){
                if (i>=0 && j>=0 && i < presenceArray.length && j < presenceArray.length){
                    System.out.println(presenceArray[j][i]);
                    System.out.println("    " + j + ", " + i);
                    if (presenceArray[j][i] == 1){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void test(){
        int[][] present = new int[][]{
                {0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,1,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0}
        };

        System.out.println(objectBelow(7,6,present,4));
    }
}
