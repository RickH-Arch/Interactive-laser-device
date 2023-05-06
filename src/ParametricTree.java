import processing.core.PApplet;
import processing.core.PVector;


import java.util.ArrayList;

import static processing.core.PApplet.*;
import static processing.core.PConstants.PI;

public class ParametricTree extends PApplet{
    public static void main(String[] args){
        PApplet.main("ParametricTree");
    }

    ArrayList<PathFinder>paths;
    public void settings() {
        size(1000, 1000);
    }
    public void setup(){
        background(0);
        ellipseMode(CENTER);
        fill(255);
        noStroke();
        smooth();
        paths = new ArrayList<PathFinder>();
        paths.add(new PathFinder());

    }

    public void draw(){
        for(int i = 0;i<paths.size();i++){
            PVector loc = paths.get(i).location;
            float diam = paths.get(i).diameter;
            ellipse(loc.x,loc.y,diam,diam);
            paths.get(i).update(this,paths);
        }
    }

    public void mousePressed(){
        background((0));
        paths = new ArrayList<PathFinder>();
        paths.add(new PathFinder());
    }

}



class PathFinder {
    PVector location;
    PVector velocity;
    float diameter;
    //PathFinder[] paths;
    PathFinder(){
        location = new PVector(500,1000);
        velocity = new PVector(0,-1);
        diameter = 32;
    }

    PathFinder(PApplet app,PathFinder parent){
        location = parent.location;
        velocity = parent.velocity;
        velocity.add(new PVector(app.random(-1,1),app.random(-1,1)).mult(0.5f));
        velocity = velocity.normalize();
        float area = PI*sq(parent.diameter/2);
        float newDiam = sqrt(area/2/PI)*2;
        diameter = newDiam;
        parent.diameter = newDiam;

    }

    public void update(PApplet app,ArrayList<PathFinder> paths){
        if(diameter>0.5){
            location.add(velocity);
            PVector bump = new PVector(app.random(-1,1),app.random(-1,1));
            bump.mult(0.1f);
            velocity.add(bump);
            velocity = velocity.normalize();
            if(app.random(0,1)<0.01){
                //paths = (PathFinder[]) append(paths,new PathFinder((this)));
                paths.add(new PathFinder(app,this));
            }
        }
    }
}


