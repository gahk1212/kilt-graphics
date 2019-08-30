package comp127graphics;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A group of graphical objects that can be added, moved, and removed as a single unit.
 * The group defines its own coordinate system, so the positions of objects added to it are relative
 * to the whole group's position.
 *
 * @author Bret Jackson
 */
public class GraphicsGroup extends GraphicsObject implements GraphicsObserver {
    /**
     * Holds the objects to be drawn in calls to paintComponent
     */
    private ConcurrentLinkedDeque<GraphicsObject> gObjects;

    /**
     * X position of group in canvas space
     */
    private double x;

    /**
     * Y position of group in canvas space
     */
    private double y;

    /**
     * Bounding rectangle around all of the graphicObjects contained in this group in window coordinates.
     */
    private java.awt.Rectangle bounds;

    /**
     * Buffer to draw the sub graphics objects on
     */
    private BufferedImage imgBuffer;
    private Graphics2D subCanvas;

    /**
     * Constructs a new group. Each group has its own local coordinate system. The group is
     * positioned on the canvas at canvas position (x, y) when it is added.
     * @param x
     * @param y
     */
    public GraphicsGroup(double x, double y){
        this.x = x;
        this.y = y;
        gObjects = new ConcurrentLinkedDeque<GraphicsObject>();
        bounds = new java.awt.Rectangle(0,0,-1, -1);
    }

    /**
     * Constructs a new group positioned at (0, 0).
     * When later used with CanvasWindow's add(GraphicsObject gObject, double x, double y), this group
     * will get placed at x, y.
     */
     public GraphicsGroup() {
        this(0.0,0.0);
    }

    /**
     * Adds given graphical object to the list of objects drawn on the canvas. The last object added
     * is the one that will appear on top.
     */
    public void add(GraphicsObject gObject){
        gObject.addObserver(this);
        gObjects.add(gObject);
        //java.awt.Rectangle objBounds = gObject.getBounds();
        bounds = bounds.union(gObject.getBounds());
        changed();
    }

    /**
     * Adds the graphical object to the list of objects drawn on the canvas
     * at the position x, y.
     *
     * @param gObject  the graphical object to be drawn
     * @param x        the x position of graphical object
     * @param y        the y position of graphical object
     */
    public void add(GraphicsObject gObject, double x, double y){
        gObject.setPosition(x, y);
        this.add(gObject);
    }

    /**
     * Removes the object from being drawn
     * @throws NoSuchElementException if gObject is not a part of the graphics group.
     */
    public void remove(GraphicsObject gObject){
        gObject.removeObserver(this);
        boolean success = gObjects.remove(gObject);
        if (!success){
            throw new NoSuchElementException("The object to remove is not part of this graphics group. It may have already been removed or was never originally added.");
        }
        changed();
    }

    /**
     * Removes all of the objects in this group
     */
    public void removeAll(){
        Iterator<GraphicsObject> it = gObjects.iterator();
        while(it.hasNext()){
            GraphicsObject obj = it.next();
            obj.removeObserver(this);
            it.remove();
        }
        changed();
    }

    /**
     * Returns the topmost graphical object underneath position x, y. If no such object exists, it returns null.
     * @param x position in the coordinate space of the container of this group
     * @param y position in the coordinate space of the container of this group
     * @return object at (x,y) or null if it does not exist.
     */
    public GraphicsObject getElementAt(double x, double y){
        Iterator<GraphicsObject> it = gObjects.descendingIterator();
        while(it.hasNext()){
            GraphicsObject obj = it.next();
            if (obj.testHit(x - this.x, y - this.y)){
                return obj;
            }
        }
        return null;
    }

    @Override
    public void draw(Graphics2D gc){
        // Don't bother drawing if nothing has been added or everything would be drawn off screen.
        if (bounds.isEmpty()) {
            return;
        }
        imgBuffer = new BufferedImage(
            Math.max(1, (int) Math.ceil(bounds.getX() + bounds.getWidth())),
            Math.max(1, (int) Math.ceil(bounds.getY() + bounds.getHeight())),
            BufferedImage.TYPE_4BYTE_ABGR);
        subCanvas = imgBuffer.createGraphics();
        enableAntialiasing();
        subCanvas.setBackground(new Color(1, 1, 1, 0));
        subCanvas.clearRect(0, 0, (int)Math.ceil(bounds.getX()+bounds.getWidth()), (int)Math.ceil(bounds.getY()+bounds.getHeight()));

        for(GraphicsObject obj: gObjects){
            obj.draw(subCanvas);
        }
        // We need to draw on the sub canvas so that getElement at works properly

        //gc.drawImage(imgBuffer, (int)x, (int)y, null);

        gc.translate(x, y);
        for(GraphicsObject obj : gObjects){
            obj.draw(gc);
        }
        gc.translate(-x, -y);

    }


    /**
     * Get the x position of the group's local coordinate system relative to the group's container.
     */
    public double getX(){
        return x;
    }

    /**
     * Get the y position of the group's local coordinate system relative to the group's container.
     */
    public double getY(){
        return y;
    }

    /**
     * Get the width of the rectangle that encloses all the elements in the group.
     */
    public double getWidth(){
        return bounds.getWidth();
    }

    /**
     * Get the height of the rectangle that encloses all the elements in the group.
     */
    public double getHeight(){
        return bounds.getHeight();
    }

    /**
     * Moves the entire group's coordinate system to (x,y).
     */
    public void setPosition(double x, double y){
        this.x = x;
        this.y = y;
        changed();
    }

    /**
     * Get the position of the group's local coordinate system relative to the group's container.
     */
    public Point getPosition(){
        return new Point(x, y);
    }

    /**
     * Tests whether the point (x, y) hits some shape inside this group.
     */
    public boolean testHit(double x, double y){
        return getElementAt(x, y) != null;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        GraphicsGroup that = (GraphicsGroup) o;
        return Double.compare(that.x, x) == 0
            && Double.compare(that.y, y) == 0
            && gObjects.equals(that.gObjects);
    }

    @Override
    public String toString(){
        return "A graphics group at position ("+getX()+", "+getY()+") with width="+getWidth()+" and height="+getHeight();
    }

    /**
     * Returns an axis aligned bounding rectangle for the graphical object in canvas coordinates.
     * @return
     */
    public java.awt.Rectangle getBounds(){
        return new java.awt.Rectangle((int) Math.ceil(this.x + bounds.getX()), (int) Math.ceil(this.y + bounds.getY()), (int) Math.ceil(bounds.getWidth()), (int) Math.ceil(bounds.getHeight()));
    }

    /**
     * Returns an iterator over the contents of this group, in the order they will be drawn.
     */
    public Iterator<GraphicsObject> iterator(){
        return gObjects.iterator();
    }

    /**
     * Implementation of GraphicsObserver method. Notifies Java to repaint the image if any of the objects drawn on the canvas
     * have changed.
     * @param changedObject
     */
    public void graphicChanged(GraphicsObject changedObject){
        updateBounds();
        changed();
    }

    /**
     * Enables antialiasing on the drawn shapes.
     */
    private void enableAntialiasing() {
        subCanvas.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        subCanvas.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        subCanvas.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
    }

    private void updateBounds(){
        java.awt.Rectangle newBounds = null;
        for(GraphicsObject gObject: gObjects) {
            if (newBounds != null) {
                newBounds = newBounds.union(gObject.getBounds());
            } else {
                java.awt.Rectangle objBounds = gObject.getBounds();
                if (objBounds != null) {
                    newBounds = new java.awt.Rectangle((int) objBounds.getX(), (int) objBounds.getY(), (int) objBounds.getWidth(), (int) objBounds.getHeight());
                }
            }
        }
        bounds = newBounds;
    }
}
