package com.jogamp.opengl.test.junit.jogl.awt;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Manual test for BufferedImage behavior w/ OSX HiDPI pixel scale usage.
 */
public class ManualHiDPIBufferedImage01AWT {

    static final int width  = 200;
    static final int height = 100;

    public static void main(final String[] args) throws InterruptedException, InvocationTargetException {
        final JFrame frame = new JFrame();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final Image image1 = getImage(getCheckBox("High-DPI (no)", false), width, height, 1);
                final Image image2 = getImage(getCheckBox("High-DPI (yes)", true), width, height, 2);
                System.err.println("Image1: "+image1);
                System.err.println("Image2: "+image2);

                @SuppressWarnings("serial")
                final Canvas canvas = new Canvas() {
                    @Override
                    public void paint(final Graphics g) {
                        super.paint(g);
                        g.drawImage(image1, 0, 0,          width, height, this);
                        g.drawImage(image2, 0, height + 5, width, height, this);
                    }
                };
                frame.getContentPane().add(getCheckBox("High-DPI (ref)", false), BorderLayout.NORTH);
                frame.getContentPane().add(canvas, BorderLayout.CENTER);

                frame.setBounds((1440-400)/2, 100, 400, 400);
                frame.validate();
                frame.setVisible(true);
            }
        });
    }

    static JCheckBox getCheckBox(final String text, final boolean selected) {
        final JCheckBox checkBox = new JCheckBox(text);
        checkBox.setSelected(selected);
        checkBox.setSize(new Dimension(width, height));
        return checkBox;
    }

    static Image getImage(final JComponent component, final int width, final int height, final int scale) {
        final BufferedImage image = new BufferedImage(width*scale, height*scale, BufferedImage.TYPE_INT_ARGB);
        final Graphics g = image.getGraphics();
        ((Graphics2D) g).scale(scale, scale);
        component.paint(g);
        g.dispose();

        return image;
    }
}
