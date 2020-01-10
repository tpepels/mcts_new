package hex.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

public class HexComponent extends JComponent implements MouseListener, MouseMotionListener {

    private int n = 11;
    private int componentWidth, componentHeight;

    private double hSize, hSizeHalf, hSizeQuarter, hSizeThreeQuarter, vSize, vSizeHalf;

    private Shape[][] hexagons;

    private int[][] hexField;

    private BufferedImage field;

    private Shape selectedHexagon;

    //edit this to change the look of the grid
    private final Stroke hexOutlineStroke = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private final Stroke outlineStroke = new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private final Stroke selectionOutlineStroke = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private final Paint backgroundPaint = new Color(0, 0, 0, 0);

    private final Paint hexBackgroundPaint = new Color(211, 215, 207);
    private final Paint hexOutlinePaint = new Color(85, 87, 83);

    private final Paint playerOnePaint = new Color(204, 0, 0);
    private final Paint playerTwoPaint = new Color(52, 101, 164);

    private final Paint selectionFillPaint = new Color(136, 138, 133);
    private final Paint selectionOutlinePaint = new Color(46, 52, 54);

    private UserInputListener listener;

    private boolean firstTurn;
    //private List<InputListener> listeners;

    public HexComponent() {
        //initiate the list of HexComponentListeners
        //listeners = new ArrayList<InputListener>();

        //add the mouse(motion)listener
        addMouseMotionListener(this);
        addMouseListener(this);
        //reset the field
        resetField();

    }

    public void paintComponent(Graphics g) {
        //first call the super method.
        super.paintComponent(g);

        Graphics2D gr = (Graphics2D) g;

        //set antialiasing to true so it will look better
        gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //if the size of the component changes, we have to recalculate the shapes
        //and redraw the field.
        if (componentWidth != getWidth() || componentHeight != getHeight()) {
            componentWidth = getWidth();
            componentHeight = getHeight();
            redrawField();
        }

        //draw the field onto the component
        gr.drawImage(field, 0, 0, null);

        synchronized (drawObject) {
            //fill all already selected fields
            if (hexField == null)
                return;

            int x, y;
            for (x = 0; x < n; x++) {
                for (y = 0; y < n; y++) {
                    if (hexField[x][y] > 0) {
                        Paint paint;
                        if (hexField[x][y] == 1)
                            paint = playerOnePaint;
                        else
                            paint = playerTwoPaint;

                        //fill it with the players color
                        gr.setPaint(paint);
                        gr.fill(hexagons[x][y]);
                    }
                }
            }


            //draw the selected hexagon, if it exists
            if (selectedHexagon != null) {
                gr.setPaint(selectionFillPaint);
                gr.fill(selectedHexagon);
                gr.setPaint(selectionOutlinePaint);
                gr.setStroke(selectionOutlineStroke);
                gr.draw(selectedHexagon);
            }
        }
    }

    private void setGridSize(int n) {
        this.n = n;
        resetField();
    }

    /*public void setValue(int x, int y, int player) {
        hexValues[x][y] = player;
        redrawField();
        repaint();
    }*/

    private void resetField() {
        //the grid of hexagons needs to be reset to the right size, n*n
        hexagons = new Shape[n][n];
    }

    private void redrawField() {
        synchronized (drawObject) {
            //create a new image with the dimensions of the field
            field = new BufferedImage(componentWidth, componentHeight,
                    BufferedImage.TYPE_INT_ARGB);

            //create a graphics object to draw on the new image
            Graphics2D gr = field.createGraphics();

            //set antialiasing to true so it will look better
            gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            //fill the background
            gr.setPaint(backgroundPaint);
            gr.fillRect(0, 0, componentWidth, componentHeight);

            //calculate the aspect ratio of the component
            double componentRatio = componentWidth / componentHeight;

            //calculate the aspect ratio of the hexField
            setSize(1.0);
            double hexFieldRatio = determineRatio();

            //compare the values to determine the final size of the hexagons
            if (hexFieldRatio < componentRatio) {

                //height of the component limits the size of the hexField
                //we therefore have to determine the size through the height
                double hexHeight = componentHeight / (double) n;
                double hexWidth = hexHeight / 0.866;
                setSize(hexWidth);
            } else {

                //width of the component limits the size of the hexField
                //we therefore have to determine the size through the width
                double hexWidthHalf = componentWidth / ((double) (2.0 * n) +
                        (double) (n - 1));

                setSize(hexWidthHalf * 2);
            }

            //bounding rectangle of the hexagonfield. Needed to determine
            //hexagon positions
            Rectangle2D hexFieldRectangle = getBoundingRectangle();

            //NOTE THAT IN THE X- AND Y-COORDINATES GIVEN TO THE "createHexagon"
            //METHOD ARE THE COORDINATES OF THE CENTER OF THE HEXAGON.

            //the middle of the hexagonfield, which is also the middle of
            //the componentwidth this is the x coordinate of the topmost
            //hexagon, which is at position 0,0
            double xStart = ((double) componentWidth / 2);

            //the y coordinate at the top of the hexagonfield, used to
            //determine the y-coordinate of the hexagons. This coordinate
            //isn't necessarily 0, it depends on the aspect ratios.
            double yStart = (((double) componentHeight / 2) - (hexFieldRectangle.getHeight() / 2));

            int x, y;
            double dx, dy;

            Path2D redOutline = new Path2D.Double();
            Path2D blueOutline = new Path2D.Double();


            for (x = 0; x < n; x++) {
                //determine the center coordinates of the first
                //hexagon in this column.
                dx = xStart + ((double) x * hSizeThreeQuarter);
                dy = yStart + ((double) (x + 1) * vSizeHalf);


                for (y = 0; y < n; y++) {

                    //add the shape to the array of shapes, needed for linking
                    //mouse input to the right hexagon.
                    hexagons[x][y] = createHexagon(dx, dy);

                    //draw the created hexagon
                    //first the fill
                    gr.setPaint(hexBackgroundPaint);
                    gr.fill(hexagons[x][y]);

                    //then the outline
                    gr.setPaint(hexOutlinePaint);
                    gr.setStroke(hexOutlineStroke);
                    gr.draw(hexagons[x][y]);


                    //determine color outlines for the players, in case we are
                    //at an outside hexagon. these paths will be drawn after
                    //these loops.
                    if (x == 0) {
                        redOutline.moveTo(dx + hSizeQuarter, dy - vSizeHalf);
                        redOutline.lineTo(dx - hSizeQuarter, dy - vSizeHalf);
                        redOutline.lineTo(dx - hSizeHalf, dy);
                    } else if (x == n - 1) {
                        redOutline.moveTo(dx + hSizeHalf, dy);
                        redOutline.lineTo(dx + hSizeQuarter, dy + vSizeHalf);
                        redOutline.lineTo(dx - hSizeQuarter, dy + vSizeHalf);
                    }
                    if (y == 0) {
                        blueOutline.moveTo(dx - hSizeQuarter, dy - vSizeHalf);
                        blueOutline.lineTo(dx + hSizeQuarter, dy - vSizeHalf);
                        blueOutline.lineTo(dx + hSizeHalf, dy);
                    } else if (y == n - 1) {
                        blueOutline.moveTo(dx - hSizeHalf, dy);
                        blueOutline.lineTo(dx - hSizeQuarter, dy + vSizeHalf);
                        blueOutline.lineTo(dx + hSizeQuarter, dy + vSizeHalf);
                    }

                    //determine the center of the next hexagon in this column.
                    dx -= hSizeThreeQuarter;
                    dy += vSizeHalf;
                }
            }

            //we are outside the loops now, so we can
            //draw the paths of the player outlines we determined earlier on.
            gr.setStroke(outlineStroke);
            gr.setPaint(playerOnePaint);
            gr.draw(redOutline);
            gr.setPaint(playerTwoPaint);
            gr.draw(blueOutline);
        }
    }

    private void setSize(double hSize) {
        //needed to ensure parts of the hexagons don't disappear
        //outside the component boundaries due to rounding
        //errors
        hSize *= .8;

        //set the variables
        this.hSize = hSize;

        hSizeHalf = hSize / 2.0;
        hSizeQuarter = hSize / 4.0;
        hSizeThreeQuarter = hSizeHalf + hSizeQuarter;

        vSize = hSizeHalf * 1.732;
        vSizeHalf = vSize / 2.0;
    }

    private double determineRatio() {
        //the ratio can be determined with a bounding rectangle
        Rectangle2D rect = getBoundingRectangle();
        return rect.getWidth() / rect.getHeight();
    }

    private Rectangle2D getBoundingRectangle() {
        //we draw the entire field into one path instead of many.
        //in this way, we can easily determine the aspect ratio
        //of the field by looking at the bounding rectangle.

        //NOTE: This might be possible with simple mathematical formulas.
        //Because these caused problems in early versions of the component,
        //this method was temporarily created to ensure the right output.

        Path2D.Double path = new Path2D.Double();

        path.moveTo(0, 0);

        int x, y;
        double dx, dy;

        for (x = 0; x < n; x++) {
            dx = (double) x * hSizeThreeQuarter;
            dy = (double) (x + 1) * vSizeHalf;
            for (y = 0; y < n; y++) {
                path.moveTo(dx - hSizeHalf, dy);
                path.lineTo(dx - hSizeQuarter, dy - vSizeHalf);
                path.lineTo(dx + hSizeQuarter, dy - vSizeHalf);
                path.lineTo(dx + hSizeHalf, dy);
                path.lineTo(dx + hSizeQuarter, dy + vSizeHalf);
                path.lineTo(dx - hSizeQuarter, dy + vSizeHalf);
                path.closePath();

                dx -= hSizeThreeQuarter;
                dy -= vSizeHalf;
            }
        }

        return path.getBounds2D();
    }

    private Shape createHexagon(double x, double y) {

        //create a new shape of the type Path2D.Double
        Path2D.Double path = new Path2D.Double();

        //draw the hexagon.
        path.moveTo(x - hSizeHalf, y);
        path.lineTo(x - hSizeQuarter, y - vSizeHalf);
        path.lineTo(x + hSizeQuarter, y - vSizeHalf);
        path.lineTo(x + hSizeHalf, y);
        path.lineTo(x + hSizeQuarter, y + vSizeHalf);
        path.lineTo(x - hSizeQuarter, y + vSizeHalf);
        path.closePath();

        //return it
        return path;
    }

    private final Object drawObject = new Object();

    public void drawField(int[][] newField) {
        synchronized (drawObject) {
            hexField = newField;
            setGridSize(newField.length);
        }
        if (componentWidth > 0 && componentHeight > 0) {
            redrawField();
            repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent me) {
        Point hexCoord = getHexCoordinate(me.getPoint());
        if (hexCoord != null && selectedHexagon != null) {
            selectedHexagon = null;
            if (listener != null)
                listener.hexSelected(hexCoord.x, hexCoord.y);
        }
    }

    public void reset() {
        listener = null;
        selectedHexagon = null;
    }

    public void startListening(UserInputListener listener, boolean firstTurn) {
        this.listener = listener;
        this.firstTurn = firstTurn;
    }

    public void stopListening() {
        this.listener = null;
    }

    private Point getHexCoordinate(Point mouseCoordinate) {
        int x, y;

        //TODO: create a shape consisting of the outline of the
        //entire field, which is to be checked first. In this way
        //we prevent the application from looping through all
        //hexagons when the mouse is on none of them.

        //loop through all hexagons in the grid and do
        //a check whether the mouse is on them.
        for (x = 0; x < n; x++) {
            for (y = 0; y < n; y++) {
                //we have a result, return the coordinate.
                if (hexagons[x][y].contains(mouseCoordinate))
                    return new Point(x, y);
            }
        }
        //mouse is outside the field, we return null.
        return null;
    }

    @Override
    public void mouseMoved(MouseEvent me) {

        synchronized (drawObject) {
            //save the old selectedHexagon
            Shape oldvalue = selectedHexagon;

            //when there are no InputListeners, there is no need to draw a selection
            if (listener == null)
                selectedHexagon = null;
            else {
                //see if the mouse is on one of the hexagons
                Point hexCoord = getHexCoordinate(me.getPoint());
                if (hexCoord != null && (hexField[hexCoord.x][hexCoord.y] == 0 || firstTurn == true)) {
                    selectedHexagon = hexagons[hexCoord.x][hexCoord.y];
                } else {
                    selectedHexagon = null;
                }
            }
            //check whether the new value of selectedHexagon
            //is different to the old one. If so, repaint the field.
            if (selectedHexagon != oldvalue)
                repaint();
        }
    }

    public void mousePressed(MouseEvent me) {
    }

    public void mouseReleased(MouseEvent me) {
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseDragged(MouseEvent me) {
    }

}
