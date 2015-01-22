package org.workcraft.plugins.fsm;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import org.workcraft.annotations.DisplayName;
import org.workcraft.annotations.Hotkey;
import org.workcraft.annotations.SVGIcon;
import org.workcraft.dom.visual.DrawRequest;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.gui.Coloriser;
import org.workcraft.gui.graph.tools.Decoration;
import org.workcraft.gui.propertyeditor.PropertyDeclaration;

@Hotkey(KeyEvent.VK_T)
@DisplayName("State")
@SVGIcon("images/icons/svg/vertex.svg")
public class VisualState extends VisualComponent {
	private static double size = 1.0;
	private static float strokeWidth = 0.1f;

	public VisualState(State state) {
		super(state);
		addPropertyDeclarations();
	}

	private void addPropertyDeclarations() {
		addPropertyDeclaration(new PropertyDeclaration<VisualState, Boolean>(
				this, "Initial", Boolean.class, true, false) {
			public void setter(VisualState object, Boolean value) {
				object.getReferencedState().setInitial(value);
			}
			public Boolean getter(VisualState object) {
				return object.getReferencedState().isInitial();
			}
		});

		addPropertyDeclaration(new PropertyDeclaration<VisualState, Boolean>(
				this, "Final", Boolean.class) {
			public void setter(VisualState object, Boolean value) {
				object.getReferencedState().setFinal(value);
			}
			public Boolean getter(VisualState object) {
				return object.getReferencedState().isFinal();
			}
		});
	}

	@Override
	public void draw(DrawRequest r) {
		Graphics2D g = r.getGraphics();
		Decoration d = r.getDecoration();

		{
			double s = size-strokeWidth;
			Shape shape = new Ellipse2D.Double(-s/2, -s/2, s, s);
			g.setColor(Coloriser.colorise(getFillColor(), d.getBackground()));
			g.fill(shape);
			g.setStroke(new BasicStroke(strokeWidth));
			g.setColor(Coloriser.colorise(getForegroundColor(), d.getColorisation()));
			g.draw(shape);
		}

		if (getReferencedState().isInitial()) {
			double s = size/4;
			Path2D shape = new Path2D.Double();
			shape.moveTo(0.0, -size);
			shape.lineTo(0.0, -size/2);
			shape.moveTo(-s/2, -size/2 - s);
			shape.lineTo(0.0, -size/2);
			shape.lineTo(s/2, -size/2 - s);
			g.setStroke(new BasicStroke(strokeWidth));
			g.setColor(Coloriser.colorise(getForegroundColor(), d.getColorisation()));
			g.draw(shape);
		}

		if (getReferencedState().isFinal()) {
			double s = 2*size/3;
			Shape shape = new Ellipse2D.Double(-s/2, -s/2, s, s);
			g.setStroke(new BasicStroke(strokeWidth/2));
			g.setColor(Coloriser.colorise(getForegroundColor(), d.getColorisation()));
			g.draw(shape);
		}

		drawLabelInLocalSpace(r);
		drawNameInLocalSpace(r);
	}

	@Override
	public boolean hitTestInLocalSpace(Point2D pointInLocalSpace) {
		return pointInLocalSpace.distanceSq(0, 0) < size * size / 4;
	}

	public State getReferencedState() {
		return (State)getReferencedComponent();
	}

}