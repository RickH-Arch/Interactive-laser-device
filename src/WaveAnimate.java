import processing.core.PApplet;

public class WaveAnimate extends PApplet{
    public static void main(String[] args){
        PApplet.main("Wave");
    }

    Particle[] particles;
    float alpha;
    int pNum = 100;

    public void settings() {
        size(620, 250);
    }
    public void setup() {

        background(0);
        noStroke();
        setParticles();
    }

    public void draw() {
        frameRate(20);
        alpha = map(mouseX, 0, width, 5, 35);
        fill(0, alpha);
        rect(0, 0, width, height);

        loadPixels();
        for (Particle p : particles) {
            p.move();
        }
        updatePixels();
    }

    void setParticles() {
        particles = new Particle[pNum];
        for (int i = 0; i < pNum; i++) {
            float x = random(width);
            float y = random(height);
            float adj = map(y, 0, height, 255, 0);
            int c = color(40, adj, 255);
            particles[i]= new Particle(x, y, c);
        }
    }

    public void mousePressed() {
        setParticles();
    }

    class Particle {
        float posX, posY, incr, theta;
        int c;

        Particle(float xIn, float yIn, int cIn) {
            posX = xIn;
            posY = yIn;
            c = cIn;
        }

        public void move() {
            update();
            wrap();
            display();
        }

        void update() {
            incr +=  .008;
            theta = noise(posX * 0.006f, posY * 0.004f, incr) * TWO_PI;
            posX += 2 * cos(theta);
            posY += 2 * sin(theta);
        }

        void display() {
            if (posX > 0 && posX < width && posY > 0  && posY < height) {
                pixels[(int)posX + (int)posY * width] =  c;
            }
        }

        void wrap() {
            if (posX < 0) posX = width;
            if (posX > width ) posX =  0;
            if (posY < 0 ) posY = height;
            if (posY > height) posY =  0;
        }
    }


}
