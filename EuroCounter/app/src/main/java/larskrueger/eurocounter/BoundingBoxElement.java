package larskrueger.eurocounter;


// Helper class for Arraylist in Imageprocessor
// stores Position, Areasize and Relevancy
public class BoundingBoxElement {
    private double area;
    private int x,y;
    private boolean isRelevant;

    public BoundingBoxElement(double a, int posX, int posY){
        this.area = a;
        this.x = posX;
        this.y = posY;
        isRelevant = true;
    }

    public double getArea() {
        return area;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean getIsRelevant(){
        return isRelevant;
    }

    public void setIsRelevant(boolean b){
        isRelevant = b;
    }
}
