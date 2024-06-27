package com.devbaltasarq.nuhvar.view.showresult;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Locale;


/** A graph created from lines. */
public class LineChart extends Drawable {
    private static double SCALED_DENSITY;
    public static final int MIN_HR = 35;
    public static final int MAX_HR = 190;

    /** Represents a single point in the graph. */
    public static final class Point {
        public Point(double x, double y)
        {
            this.x = x;
            this.y = y;
        }

        /** @return the x coordinate. */
        public double getX()
        {
            return this.x;
        }

        /** @return the y coordinate. */
        public double getY()
        {
            return this.y;
        }

        @Override
        public String toString()
        {
            return String.format (Locale.getDefault(),
                    "(%06.2f, %06.2f)",
                    this.getX(), this.getY() );
        }

        private final double x;
        private final double y;
    }

    /** Constructs a new graph. */
    public LineChart(double scaledDensity,
                     Collection<Point> points,
                     int graphCcolor)
    {
        // Set up graphics
        SCALED_DENSITY = scaledDensity;
        this.drawGrid = true;
        this.paint = new Paint();
        this.paint.setStrokeWidth( 2 );

        // Obtain data
        this.minHR = MIN_HR;
        this.maxHR = MAX_HR;
        this.graphColor = graphCcolor;
        this.points = points.toArray( new Point[ 0 ] );

        // Preparation for data normalization
        this.normalize = false;
        this.calculateFixedYAxis();

        this.showLabels = true;
        this.labelThreshold = 1.0;
    }

    /** @return the color, as an int. */
    public int getGraphColor()
    {
        return this.graphColor;
    }

    /** @return true if the data should be normalized, false otherwise. */
    public boolean getNormalize()
    {
        return this.normalize;
    }

    public void setNormalize(boolean val)
    {
        this.normalize = val;

        if ( this.normalize ) {
            this.normalizeYAxis();
        } else {
            this.calculateFixedYAxis();
        }
    }

    /** Calculates the minimum and maximum values in the data sets,
      * provided we want a fixed y axis.
      * This is stored in the minX, minY, maxX and maxY
      * @see Point
      */
    private void calculateFixedYAxis()
    {
        if ( this.points.length > 0 ) {
            // Time (x axis)
            this.minX = this.points[ 0 ].getX();
            this.maxX = this.points[ this.points.length - 1 ].getX();

            // This sets the y axis for the following values
            this.minY = this.minHR;
            this.maxY = this.maxHR;
        }

        return;
    }

    /** Calculates the minimum and maximum values in the data set.
     * This is stored in the minX, minY, maxX and maxY
     * @see Point
     */
    private void normalizeYAxis()
    {
        final int LENGTH_POINTS = this.points.length;

        if ( LENGTH_POINTS > 0 ) {
            // Time (x axis)
            this.minX = this.points[ 0 ].getX();
            this.maxX = this.points[ this.points.length - 1 ].getX();

            // HR (Y axis)
            this.minY = this.maxY = this.points[ 0 ].getY();
            for(int i = 1; i < LENGTH_POINTS; ++i) {
                final double HR = this.points[ i ].getY();

                if ( HR > this.maxY ) {
                    this.maxY = HR;
                }

                if ( HR < this.minY ) {
                    this.minY = HR;
                }
            }
        }

        return;
    }

    @Override
    public void setAlpha(int x)
    {
    }

    @Override
    public int getOpacity()
    {
        return PixelFormat.OPAQUE;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter)
    {
    }

    @Override
    protected boolean onLevelChange(int level)
    {
        super.onLevelChange( level );

        this.invalidateSelf();
        return true;
    }

    /** @return the minimum drawable HR. */
    public int getMinHR()
    {
        return this.minHR;
    }

    /** @return the maximum drawable HR. */
    public int getMaxHR()
    {
        return this.maxHR;
    }

    /** Sets the minimum drawable HR.
      * @param val the new HR value.
      */
    public void setMinHR(int val)
    {
        this.minHR = val;
        this.setNormalize( false );
    }

    /** Sets the maximum drawable HR.
     * @param val the new HR value.
     */
    public void setMaxHR(int val)
    {
        this.maxHR = val;
        this.setNormalize( false );
    }

    /** Sets the legend for the x axis. */
    public void setLegendX(String legendX)
    {
        this.legendX = legendX;
    }

    /** Sets the legend for the y axis. */
    public void setLegendY(String legendY)
    {
        this.legendY = legendY;
    }

    /** @return true if the grid will be drawn, false otherwise. */
    public boolean shouldDrawGrid()
    {
        return this.drawGrid;
    }

    /** @return true if the labels for each point should be shown, false otherwise. */
    public boolean shouldShowLabels()
    {
        return this.showLabels;
    }

    /** Sets the label threshold. */
    public void setLabelThreshold(double threshold)
    {
        this.labelThreshold = threshold;
    }

    /** Shows the labels on the chart or not. */
    public void setShowLabels(boolean showLabels)
    {
        this.showLabels = showLabels;
    }

    /** Changes whether the grid should be drawn or not.
      * @param drawGrid true to draw the grid, false otherwise.
      */
    public void setDrawGrid(boolean drawGrid)
    {
        this.drawGrid = drawGrid;
    }

    @Override
    public void draw(@NonNull Canvas canvas)
    {
        final int LEGEND_SPACE = 50;
        final int CHART_PADDING = 60;
        final float TEXT_SIZE_SP = 10;
        final float TEXT_SIZE = TEXT_SIZE_SP * (float) SCALED_DENSITY;

        // Set up
        this.canvas = canvas;
        this.chartBounds = new Rect( 0,  0, this.canvas.getWidth(), this.canvas.getHeight() );
        this.paint.setTypeface( Typeface.create( "serif", Typeface.NORMAL ) );
        this.paint.setTextSize( TEXT_SIZE );
        this.paint.setAntiAlias( true );

        // Adjust chart bounds
        this.chartBounds.top += CHART_PADDING;
        this.chartBounds.right -= CHART_PADDING;
        this.chartBounds.bottom -= CHART_PADDING + LEGEND_SPACE;
        this.chartBounds.left += CHART_PADDING + LEGEND_SPACE;

        // Draw the graph's axis
        this.paint.setStrokeWidth( 6 );
        this.drawAxis();
        this.drawGrid();

        // Draw the data
        this.paint.setStrokeWidth( 4 );
        this.drawData();
    }

    /** Draws a new line in the canvas.
      * Remember to set the canvas attribute (in LineChart::draw) before using this method.
      * @param x the initial x coordinate.
      * @param y the initial y coordinate.
      * @param x2 the final x coordinate.
      * @param y2 the final y coordinate
      * @param color the color to draw the line with.
      * @see LineChart::draw
      */
    private void line(int x, int y, int x2, int y2, int color)
    {
        this.paint.setColor( color );
        canvas.drawLine( x, y, x2, y2, this.paint );
    }

    /** Writes a real value as text.
     * Remember to set the canvas attribute (in LineChart::draw) before using this method.
     * @param x the horizontal coordinate
     * @param y the vertical coordinate
     * @param value the value to show.
     */
    private void write(double x, double y, double value)
    {
        String strNum;

        if ( ( value == Math.floor( value ) )
          && !Double.isInfinite( value ) )
        {
            // It is actually an integer number
            strNum = String.format( Locale.getDefault(), "%02d", (int) value );
        } else {
            strNum = String.format( Locale.getDefault(), "%04.1f", value );
        }

        this.write( x, y, strNum );
    }

    /** Writes a real value as text.
      * Remember to set the canvas attribute (in LineChart::draw) before using this method.
      * @param x the horizontal coordinate
      * @param y the vertical coordinate
      * @param msg the string to show.
      */
    private void write(double x, double y, String msg)
    {
        final float BEFORE_STROKE_WIDTH = this.paint.getStrokeWidth();
        final int BEFORE_COLOR = this.paint.getColor();

        this.paint.setStrokeWidth( 1 );

        this.paint.setColor( Color.WHITE );
        this.canvas.drawText( msg, (float) x, (float) y, this.paint );

        this.paint.setColor( Color.BLACK );
        this.canvas.drawText( msg, (float) x + 3, (float) y + 3, this.paint );

        this.paint.setColor( BEFORE_COLOR );
        this.paint.setStrokeWidth( BEFORE_STROKE_WIDTH );
    }

    /** Draws the grid.
      * @see LineChart::shouldDrawGrid
      */
    private void drawGrid()
    {
        if ( this.shouldDrawGrid() ) {
            final int NUM_SLOTS = 10;
            final int CHART_RIGHT = this.chartBounds.right;
            final int CHART_LEFT = this.chartBounds.left;
            final int CHART_TOP = this.chartBounds.top;
            final int CHART_BOTTOM = this.chartBounds.bottom;

            this.paint.setStrokeWidth( 1 );
            this.paint.setAlpha( 100 );

            // Complete rectangle
            this.line( CHART_LEFT, CHART_TOP, CHART_RIGHT, CHART_TOP, Color.WHITE );
            this.line( CHART_RIGHT, CHART_TOP, CHART_RIGHT, CHART_BOTTOM, Color.WHITE );
            this.line( CHART_LEFT, CHART_TOP - 1, CHART_RIGHT, CHART_TOP - 1, Color.BLACK );
            this.line( CHART_RIGHT - 1, CHART_TOP, CHART_RIGHT - 1, CHART_BOTTOM, Color.BLACK );

            // Intermediate vertical lines (marking the x segments)
            final double SLOT_DATA_X = ( this.maxX - this.minX ) / NUM_SLOTS;

            for(int i = 1; i <= NUM_SLOTS; ++i) {
                final double DATA_X = this.minX + ( SLOT_DATA_X * i );
                final int X = this.translateX( DATA_X );

                this.write( X - 20, CHART_BOTTOM + 35, DATA_X );
                this.line( X, CHART_BOTTOM, X, CHART_TOP, Color.WHITE );
                this.line( X - 1, CHART_BOTTOM, X - 1, CHART_TOP, Color.BLACK );
            }

            // Intermediate horizontal lines (marking the y segments)
            final double SLOT_DATA_Y = ( this.maxY - this.minY ) / NUM_SLOTS;

            for(int i = 1; i < NUM_SLOTS; ++i) {
                final double DATA_Y = this.minY + ( i * SLOT_DATA_Y );
                final int Y = this.translateY( DATA_Y );

                this.write( CHART_LEFT - 60, Y, DATA_Y );
                this.line( CHART_LEFT, Y, CHART_RIGHT, Y, Color.WHITE );
                this.line( CHART_LEFT, Y - 1, CHART_RIGHT, Y - 1, Color.BLACK );
            }
        }

        this.paint.setAlpha( 255 );
    }

    /** @return the normalized value for x, right for drawing in the screen. */
    private int translateX(double x)
    {
        final double X = x - this.minX;
        final int NORM_X = (int) ( ( X * this.chartBounds.width() ) / ( this.maxX - this.minX ) );

        return this.chartBounds.left + NORM_X;
    }

    /** @return the normalized value for y, right for drawing in the screen. */
    private int translateY(double y)
    {
        final double Y = y - this.minY;
        final int NORM_Y = (int) ( ( Y * this.chartBounds.height() ) / ( this.maxY - this.minY ) );

        return this.chartBounds.bottom - NORM_Y;
    }

    /** Draws the axis of the graph */
    private void drawAxis()
    {
        final int LEFT = this.chartBounds.left;
        final int BOTTOM = this.chartBounds.bottom;

        // Horizontal axis
        this.line( LEFT, BOTTOM, this.chartBounds.right, BOTTOM, Color.BLACK );
        this.line( LEFT, BOTTOM + 1, this.chartBounds.right, BOTTOM + 1, Color.WHITE );

        // Vertical axis
        this.line( LEFT, this.chartBounds.top, LEFT, BOTTOM, Color.BLACK );
        this.line( LEFT - 1, this.chartBounds.top, LEFT - 1, BOTTOM + 1, Color.WHITE );

        // Vertical legend
        float textWidthY = this.paint.measureText( this.legendY );
        int centeredLegendY = ( this.chartBounds.height() / 2 ) - ( (int) ( textWidthY / 2 ) );
        int posLegendYX = LEFT - 70;
        int posLegendYY = BOTTOM - centeredLegendY;
        this.canvas.save();
        this.canvas.rotate( -90, posLegendYX, posLegendYY );
        this.write( posLegendYX, posLegendYY, this.legendY );
        this.canvas.restore();

        // Horizontal legend
        float textWidthX = this.paint.measureText( this.legendX );
        int posLegendX = ( this.chartBounds.width() / 2 ) - ( (int) ( textWidthX / 2 ) );
        this.write( LEFT + posLegendX, BOTTOM + 60, this.legendX );
    }

    /** Draws the data in the chart. */
    private void drawData()
    {
        final android.graphics.Point PREVIOUS_POINT = new android.graphics.Point( -1, -1 );
        int numLabelsShown = 0;
        double lastY = Double.MIN_VALUE;


        for(Point point : this.points) {
            final double DATA_Y = point.getY();
            final int X = this.translateX( point.getX() );
            final int Y = this.translateY( DATA_Y );

            if ( PREVIOUS_POINT.x >= 0 ) {
                this.line( PREVIOUS_POINT.x, PREVIOUS_POINT.y, X, Y, this.getGraphColor() );

                // Show label only if it's different than the previous one
                if ( ( this.shouldShowLabels()
                    && ( Math.abs( DATA_Y - lastY ) > this.labelThreshold ) )
                  || ( numLabelsShown < 2
                     && ( DATA_Y == this.minY
                     || DATA_Y == this.maxY ) ) )
                {
                    this.write( X + 10, Y - 10, DATA_Y );
                    ++numLabelsShown;
                }
            }

            PREVIOUS_POINT.x = X;
            PREVIOUS_POINT.y = Y;
            lastY = DATA_Y;
        }

        return;
    }

    private final int graphColor;
    private String legendX;
    private String legendY;
    private boolean drawGrid;
    private Rect chartBounds;
    private final Paint paint;
    private Canvas canvas;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private int maxHR;
    private int minHR;
    private final Point[] points;
    private boolean showLabels;
    private double labelThreshold;
    private boolean normalize;
}
