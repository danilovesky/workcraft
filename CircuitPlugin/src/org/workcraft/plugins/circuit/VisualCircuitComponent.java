package org.workcraft.plugins.circuit;

import org.workcraft.dom.Container;
import org.workcraft.dom.DefaultGroupImpl;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.*;
import org.workcraft.utils.Coloriser;
import org.workcraft.gui.tools.Decoration;
import org.workcraft.gui.properties.PropertyDeclaration;
import org.workcraft.observation.*;
import org.workcraft.plugins.circuit.VisualContact.Direction;
import org.workcraft.plugins.builtin.settings.CommonVisualSettings;
import org.workcraft.utils.Hierarchy;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.*;

public class VisualCircuitComponent extends VisualComponent implements Container, CustomTouchable, StateObserver, ObservableHierarchy {

    public static final String PROPERTY_RENDER_TYPE = "Render type";

    private static final Color inputColor = VisualContact.inputColor;
    private static final Color outputColor = VisualContact.outputColor;

    double labelMargin = 0.2;
    double contactLength = 0.5;
    double contactStep = 1.0;
    double contactMargin = 0.5;

    public Rectangle2D internalBB = null;
    public DefaultGroupImpl groupImpl = new DefaultGroupImpl(this);

    private final HashMap<VisualContact, GlyphVector> contactLableGlyphs = new HashMap<>();

    public VisualCircuitComponent(CircuitComponent component) {
        super(component, true, true, true);
        component.addObserver(this);
        addPropertyDeclarations();
    }

    private void addPropertyDeclarations() {
        addPropertyDeclaration(new PropertyDeclaration<VisualCircuitComponent, Boolean>(
                this, CircuitComponent.PROPERTY_IS_ENVIRONMENT, Boolean.class, true, true) {
            @Override
            public void setter(VisualCircuitComponent object, Boolean value) {
                object.setIsEnvironment(value);
            }
            @Override
            public Boolean getter(VisualCircuitComponent object) {
                return object.getIsEnvironment();
            }
        });

        addPropertyDeclaration(new PropertyDeclaration<VisualCircuitComponent, Boolean>(
                this, Contact.PROPERTY_PATH_BREAKER, Boolean.class, true, true) {
            @Override
            public void setter(VisualCircuitComponent object, Boolean value) {
                object.getReferencedComponent().setPathBreaker(value);
            }
            @Override
            public Boolean getter(VisualCircuitComponent object) {
                return object.getReferencedComponent().getPathBreaker();
            }
        });
// TODO: Rename label to module name (?)
//        renamePropertyDeclarationByName(PROPERTY_LABEL, CircuitComponent.PROPERTY_MODULE);
    }

    @Override
    public CircuitComponent getReferencedComponent() {
        return (CircuitComponent) super.getReferencedComponent();
    }

    public boolean getIsEnvironment() {
        if (getReferencedComponent() != null) {
            return getReferencedComponent().getIsEnvironment();
        }
        return false;
    }

    public void setIsEnvironment(boolean value) {
        if (getReferencedComponent() != null) {
            getReferencedComponent().setIsEnvironment(value);
        }
    }

    private LinkedList<VisualContact> getOrderedOutsideContacts(Direction dir) {
        LinkedList<VisualContact> list = new LinkedList<>();
        Rectangle2D bb = getInternalBoundingBoxInLocalSpace();
        for (VisualContact vc : Hierarchy.getChildrenOfType(this, VisualContact.class)) {
            if ((vc.getDirection() == dir) && !bb.contains(vc.getX(), vc.getY())) {
                list.add(vc);
            }
        }
        Collections.sort(list, (vc1, vc2) -> {
            if ((dir == Direction.NORTH) || (dir == Direction.SOUTH)) {
                return Double.compare(vc1.getX(), vc2.getX());
            } else {
                return Double.compare(vc1.getY(), vc2.getY());
            }
        });
        return list;
    }

    private int getContactCount(final Direction dir) {
        int count = 0;
        for (VisualContact vc: Hierarchy.getChildrenOfType(this, VisualContact.class)) {
            if (vc.getDirection() == dir) {
                count++;
            }
        }
        return count;
    }

    private void spreadContactsEvenly() {
        int westCount = getContactCount(Direction.WEST);
        int northCount = getContactCount(Direction.NORTH);
        int eastCount = getContactCount(Direction.EAST);
        int southCount = getContactCount(Direction.SOUTH);

        double westPosition = -contactStep * (westCount - 1) / 2;
        double northPosition = -contactStep * (northCount - 1) / 2;
        double eastPosition = -contactStep * (eastCount - 1) / 2;
        double southPosition = -contactStep * (southCount - 1) / 2;
        for (VisualContact vc: Hierarchy.getChildrenOfType(this, VisualContact.class)) {
            switch (vc.getDirection()) {
            case WEST:
                vc.setY(westPosition);
                westPosition += contactStep;
                break;
            case NORTH:
                vc.setX(northPosition);
                northPosition += contactStep;
                break;
            case EAST:
                vc.setY(eastPosition);
                eastPosition += contactStep;
                break;
            case SOUTH:
                vc.setX(southPosition);
                southPosition += contactStep;
                break;
            }
        }
        invalidateBoundingBox();
    }

    public Collection<VisualContact> getContacts() {
        return Hierarchy.getChildrenOfType(this, VisualContact.class);
    }

    public void setContactsDefaultPosition() {
        spreadContactsEvenly();

        Rectangle2D bb = getInternalBoundingBoxInLocalSpace();

        Collection<VisualContact> contacts = getContacts();
        for (VisualContact vc: contacts) {
            switch (vc.getDirection()) {
            case WEST:
                vc.setX(bb.getMinX() - contactLength);
                break;
            case NORTH:
                vc.setY(bb.getMinY() - contactLength);
                break;
            case EAST:
                vc.setX(bb.getMaxX() + contactLength);
                break;
            case SOUTH:
                vc.setY(bb.getMaxY() + contactLength);
                break;
            }
        }
        invalidateBoundingBox();
    }

    @Override
    public void centerPivotPoint(boolean horisontal, boolean vertical) {
        super.centerPivotPoint(horisontal, vertical);
        invalidateBoundingBox();
    }

    public VisualContact createContact(Contact.IOType ioType) {
        VisualFunctionContact vc = new VisualFunctionContact(new FunctionContact(ioType));
        addContact(vc);
        return vc;
    }

    public void addContact(VisualContact vc) {
        if (!getChildren().contains(vc)) {
            getReferencedComponent().add(vc.getReferencedComponent());
            add(vc);
        }
    }

    public void setPositionByDirection(VisualContact vc, Direction direction, boolean reverseProgression) {
        vc.setDirection(direction);
        Rectangle2D bb = getContactExpandedBox();
        switch (vc.getDirection()) {
        case WEST:
            vc.setX(TransformHelper.snapP5(bb.getMinX() - contactLength));
            positionVertical(vc, reverseProgression);
            break;
        case NORTH:
            vc.setY(TransformHelper.snapP5(bb.getMinY() - contactLength));
            positionHorizontal(vc, reverseProgression);
            break;
        case EAST:
            vc.setX(TransformHelper.snapP5(bb.getMaxX() + contactLength));
            positionVertical(vc, reverseProgression);
            break;
        case SOUTH:
            vc.setY(TransformHelper.snapP5(bb.getMaxY() + contactLength));
            positionHorizontal(vc, reverseProgression);
            break;
        }
        centerPivotPoint(true, true);
        invalidateBoundingBox();
    }

    private void positionHorizontal(VisualContact vc, boolean reverseProgression) {
        LinkedList<VisualContact> contacts = getOrderedOutsideContacts(vc.getDirection());
        contacts.remove(vc);
        if (contacts.size() == 0) {
            vc.setX(0);
        } else {
            if (reverseProgression) {
                double x = TransformHelper.snapP5(contacts.getFirst().getX() - contactStep);
                for (VisualContact contact : getOrderedOutsideContacts(Direction.WEST)) {
                    if (contact.getX() > x - contactMargin - contactLength) {
                        contact.setX(x - contactMargin - contactLength);
                    }
                }
                vc.setX(x);
            } else {
                double x = TransformHelper.snapP5(contacts.getLast().getX() + contactStep);
                for (VisualContact contact : getOrderedOutsideContacts(Direction.EAST)) {
                    if (contact.getX() < x + contactMargin + contactLength) {
                        contact.setX(x + contactMargin + contactLength);
                    }
                }
                vc.setX(x);
            }
        }
    }

    private void positionVertical(VisualContact vc, boolean reverseProgression) {
        LinkedList<VisualContact> contacts = getOrderedOutsideContacts(vc.getDirection());
        contacts.remove(vc);
        if (contacts.size() == 0) {
            vc.setY(0);
        } else {
            if (reverseProgression) {
                double y = TransformHelper.snapP5(contacts.getFirst().getY() - contactStep);
                for (VisualContact contact : getOrderedOutsideContacts(Direction.NORTH)) {
                    if (contact.getY() > y - contactMargin - contactLength) {
                        contact.setY(y - contactMargin - contactLength);
                    }
                }
                vc.setY(y);
            } else {
                double y = TransformHelper.snapP5(contacts.getLast().getY() + contactStep);
                for (VisualContact contact : getOrderedOutsideContacts(Direction.SOUTH)) {
                    if (contact.getY() < y + contactMargin + contactLength) {
                        contact.setY(y + contactMargin + contactLength);
                    }
                }
                vc.setY(y);
            }
        }
    }

    public void invalidateBoundingBox() {
        internalBB = null;
    }

    private Rectangle2D getContactMinimalBox() {
        double size = CommonVisualSettings.getNodeSize();
        double xMin = -size / 2;
        double yMin = -size / 2;
        double xMax = size / 2;
        double yMax = size / 2;
        for (VisualContact vc: Hierarchy.getChildrenOfType(this, VisualContact.class)) {
            switch (vc.getDirection()) {
            case WEST:
                double xWest = vc.getX() + contactLength;
                if ((xWest < -size / 2) && (xWest > xMin)) {
                    xMin = xWest;
                }
                break;
            case NORTH:
                double yNorth = vc.getY() + contactLength;
                if ((yNorth < -size / 2) && (yNorth > yMin)) {
                    yMin = yNorth;
                }
                break;
            case EAST:
                double xEast = vc.getX() - contactLength;
                if ((xEast > size / 2) && (xEast < xMax)) {
                    xMax = xEast;
                }
                break;
            case SOUTH:
                double ySouth = vc.getY() - contactLength;
                if ((ySouth > size / 2) && (ySouth < yMax)) {
                    yMax = ySouth;
                }
                break;
            }
        }
        return new Rectangle2D.Double(xMin, yMin, xMax - xMin, yMax - yMin);
    }

    private Rectangle2D getContactExpandedBox() {
        Rectangle2D minBox = getContactMinimalBox();
        double x1 = minBox.getMinX();
        double y1 = minBox.getMinY();
        double x2 = minBox.getMaxX();
        double y2 = minBox.getMaxY();
        for (VisualContact vc: Hierarchy.getChildrenOfType(this, VisualContact.class)) {
            double x = vc.getX();
            double y = vc.getY();
            switch (vc.getDirection()) {
            case WEST:
                if (vc.getX() < minBox.getMinX()) {
                    y1 = Math.min(y1, y - contactMargin);
                    y2 = Math.max(y2, y + contactMargin);
                }
                break;
            case NORTH:
                if (vc.getY() < minBox.getMinY()) {
                    x1 = Math.min(x1, x - contactMargin);
                    x2 = Math.max(x2, x + contactMargin);
                }
                break;
            case EAST:
                if (vc.getX() > minBox.getMaxX()) {
                    y1 = Math.min(y1, y - contactMargin);
                    y2 = Math.max(y2, y + contactMargin);
                }
                break;
            case SOUTH:
                if (vc.getY() > minBox.getMaxY()) {
                    x1 = Math.min(x1, x - contactMargin);
                    x2 = Math.max(x2, x + contactMargin);
                }
                break;
            }
        }
        return new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);
    }

    private Rectangle2D getContactBestBox() {
        Rectangle2D expBox = getContactExpandedBox();
        double x1 = expBox.getMinX();
        double y1 = expBox.getMinY();
        double x2 = expBox.getMaxX();
        double y2 = expBox.getMaxY();

        boolean westFirst = true;
        boolean northFirst = true;
        boolean eastFirst = true;
        boolean southFirst = true;

        for (VisualContact vc: Hierarchy.getChildrenOfType(this, VisualContact.class)) {
            double x = vc.getX();
            double y = vc.getY();
            switch (vc.getDirection()) {
            case WEST:
                if (westFirst) {
                    x1 = x + contactLength;
                } else {
                    x1 = Math.max(x1, x + contactLength);
                }
                westFirst = false;
                break;
            case NORTH:
                if (northFirst) {
                    y1 = y + contactLength;
                } else {
                    y1 = Math.max(y1, y + contactLength);
                }
                northFirst = false;
                break;
            case EAST:
                if (eastFirst) {
                    x2 = x - contactLength;
                } else {
                    x2 = Math.min(x2, x - contactLength);
                }
                eastFirst = false;
                break;
            case SOUTH:
                if (southFirst) {
                    y2 = y - contactLength;
                } else {
                    y2 = Math.min(y2, y - contactLength);
                }
                southFirst = false;
                break;
            }
        }

        if (x1 > x2) {
            x1 = x2 = (x1 + x2) / 2;
        }
        if (y1 > y2) {
            y1 = y2 = (y1 + y2) / 2;
        }
        Rectangle2D maxBox = new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);
        return BoundingBoxHelper.union(expBox, maxBox);
    }

    private Point2D getContactLinePosition(VisualContact vc) {
        Point2D result = null;
        Rectangle2D bb = getInternalBoundingBoxInLocalSpace();
        switch (vc.getDirection()) {
        case NORTH:
            result = new Point2D.Double(vc.getX(), bb.getMinY());
            break;
        case EAST:
            result = new Point2D.Double(bb.getMaxX(), vc.getY());
            break;
        case SOUTH:
            result = new Point2D.Double(vc.getX(), bb.getMaxY());
            break;
        case WEST:
            result = new Point2D.Double(bb.getMinX(), vc.getY());
            break;
        }
        return result;
    }

    private void drawContactLines(DrawRequest r) {
        for (VisualContact vc: Hierarchy.getChildrenOfType(this, VisualContact.class)) {
            Point2D p1 = vc.getPosition();
            Point2D p2 = getContactLinePosition(vc);
            if ((p1 != null) && (p2 != null)) {
                Graphics2D g = r.getGraphics();
                Decoration d = r.getDecoration();
                Color colorisation = d.getColorisation();
                g.setStroke(new BasicStroke((float) CircuitSettings.getWireWidth()));
                g.setColor(Coloriser.colorise(getForegroundColor(), colorisation));
                Line2D line = new Line2D.Double(p1, p2);
                g.draw(line);
            }
        }
    }

    private GlyphVector getContactLabelGlyphs(DrawRequest r, VisualContact vc) {
        Circuit circuit = (Circuit) r.getModel().getMathModel();
        String name = circuit.getName(vc.getReferencedContact());
        final FontRenderContext context = new FontRenderContext(AffineTransform.getScaleInstance(1000.0, 1000.0), true, true);
        GlyphVector gv = contactLableGlyphs.get(vc);
        if (gv == null) {
            gv = getNameFont().createGlyphVector(context, name);
            contactLableGlyphs.put(vc, gv);
        }
        return gv;
    }

    private void drawContactLabel(DrawRequest r, VisualContact vc) {
        Graphics2D g = r.getGraphics();
        Decoration d = r.getDecoration();
        Color colorisation = d.getColorisation();
        Color color = vc.isInput() ? inputColor : outputColor;
        g.setColor(Coloriser.colorise(color, colorisation));

        Rectangle2D bb = getInternalBoundingBoxInLocalSpace();
        GlyphVector gv = getContactLabelGlyphs(r, vc);
        Rectangle2D labelBB = gv.getVisualBounds();

        float labelX = 0.0f;
        float labelY = 0.0f;
        switch (vc.getDirection()) {
        case NORTH:
            labelX = (float) (-bb.getMinY() - labelMargin - labelBB.getWidth());
            labelY = (float) (vc.getX() + labelBB.getHeight() / 2);
            break;
        case EAST:
            labelX = (float) (bb.getMaxX() - labelMargin - labelBB.getWidth());
            labelY = (float) (vc.getY() + labelBB.getHeight() / 2);
            break;
        case SOUTH:
            labelX = (float) (-bb.getMaxY() + labelMargin);
            labelY = (float) (vc.getX() + labelBB.getHeight() / 2);
            break;
        case WEST:
            labelX = (float) (bb.getMinX() + labelMargin);
            labelY = (float) (vc.getY() + labelBB.getHeight() / 2);
            break;
        }
        g.drawGlyphVector(gv, labelX, labelY);
    }

    public void drawContactLabels(DrawRequest r) {
        Graphics2D g = r.getGraphics();
        AffineTransform savedTransform = g.getTransform();

        for (VisualContact vc: Hierarchy.getChildrenOfType(this, VisualContact.class,
                contact -> (contact.getDirection() == Direction.WEST) || (contact.getDirection() == Direction.EAST))) {
            drawContactLabel(r, vc);
        }

        AffineTransform rotateTransform = new AffineTransform();
        rotateTransform.quadrantRotate(-1);
        g.transform(rotateTransform);

        for (VisualContact vc: Hierarchy.getChildrenOfType(this, VisualContact.class,
                contact -> (contact.getDirection() == Direction.NORTH) || (contact.getDirection() == Direction.SOUTH))) {

            drawContactLabel(r, vc);
        }

        g.setTransform(savedTransform);
    }

    @Override
    public Rectangle2D getInternalBoundingBoxInLocalSpace() {
        if ((groupImpl != null) && (internalBB == null)) {
            internalBB = getContactBestBox();
        }
        if (internalBB != null) {
            return BoundingBoxHelper.copy(internalBB);
        }
        return super.getInternalBoundingBoxInLocalSpace();
    }

    @Override
    public Rectangle2D getBoundingBoxInLocalSpace() {
        Rectangle2D bb = super.getBoundingBoxInLocalSpace();
        Collection<Touchable> touchableChildren = Hierarchy.getChildrenOfType(this, Touchable.class);
        Rectangle2D childrenBB = BoundingBoxHelper.mergeBoundingBoxes(touchableChildren);
        return BoundingBoxHelper.union(bb, childrenBB);
    }

    @Override
    public void draw(DrawRequest r) {
        // Cache rendered text to better estimate the bounding box
        cacheRenderedText(r);
        drawOutline(r);
        drawPivot(r);
        drawContactLines(r);
        drawContactLabels(r);
        drawLabelInLocalSpace(r);
        drawNameInLocalSpace(r);
        // External decorations
        Graphics2D g = r.getGraphics();
        Decoration d = r.getDecoration();
        d.decorate(g);
    }

    @Override
    public void drawOutline(DrawRequest r) {
        Decoration d = r.getDecoration();
        Graphics2D g = r.getGraphics();
        Rectangle2D bb = getInternalBoundingBoxInLocalSpace();
        if (bb != null) {
            g.setColor(Coloriser.colorise(getFillColor(), d.getBackground()));
            g.fill(bb);
            g.setColor(Coloriser.colorise(getForegroundColor(), d.getColorisation()));
            setStroke(g);
            g.draw(bb);
        }
    }

    public void setStroke(Graphics2D g) {
        if (getIsEnvironment()) {
            float[] pattern = {0.2f, 0.2f};
            g.setStroke(new BasicStroke((float) CircuitSettings.getBorderWidth(),
                        BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1.0f, pattern, 0.0f));
        } else {
            g.setStroke(new BasicStroke((float) CircuitSettings.getBorderWidth()));
        }
    }

    @Override
    public void add(Node node) {
        groupImpl.add(node);
        if (node instanceof VisualContact) {
            ((VisualContact) node).addObserver(this);
        }
    }

    @Override
    public Collection<Node> getChildren() {
        return groupImpl.getChildren();
    }

    @Override
    public Node getParent() {
        return groupImpl.getParent();
    }

    @Override
    public void setParent(Node parent) {
        groupImpl.setParent(parent);
    }

    @Override
    public void remove(Node node) {
        if (node instanceof VisualContact) {
            invalidateBoundingBox();
            contactLableGlyphs.remove(node);
        }
        groupImpl.remove(node);
    }

    @Override
    public void add(Collection<? extends Node> nodes) {
        groupImpl.add(nodes);
        for (Node node : nodes) {
            if (node instanceof VisualContact) {
                ((VisualContact) node).addObserver(this);
            }
        }
    }

    @Override
    public void remove(Collection<? extends Node> nodes) {
        for (Node n : nodes) {
            remove(n);
        }
    }

    @Override
    public void reparent(Collection<? extends Node> nodes, Container newParent) {
        groupImpl.reparent(nodes, newParent);
    }

    @Override
    public void reparent(Collection<? extends Node> nodes) {
        groupImpl.reparent(nodes);
    }

    @Override
    public Node hitCustom(Point2D point) {
        Point2D pointInLocalSpace = getParentToLocalTransform().transform(point, null);
        for (Node node : getChildren()) {
            if (node instanceof VisualNode) {
                VisualNode vn = (VisualNode) node;
                if (vn.hitTest(pointInLocalSpace)) {
                    return vn;
                }
            }
        }
        return hitTest(point) ? this : null;
    }

    @Override
    public void notify(StateEvent e) {
        if (e instanceof TransformChangedEvent) {
            TransformChangedEvent t = (TransformChangedEvent) e;
            if (t.sender instanceof VisualContact) {
                VisualContact vc = (VisualContact) t.sender;

                AffineTransform at = t.sender.getTransform();
                double x = at.getTranslateX();
                double y = at.getTranslateY();
                Rectangle2D bb = getContactExpandedBox();
                if ((x <= bb.getMinX()) && (y > bb.getMinY()) && (y < bb.getMaxY())) {
                    vc.setDirection(Direction.WEST);
                }
                if ((x >= bb.getMaxX()) && (y > bb.getMinY()) && (y < bb.getMaxY())) {
                    vc.setDirection(Direction.EAST);
                }
                if ((y <= bb.getMinY()) && (x > bb.getMinX()) && (x < bb.getMaxX())) {
                    vc.setDirection(Direction.NORTH);
                }
                if ((y >= bb.getMaxY()) && (x > bb.getMinX()) && (x < bb.getMaxX())) {
                    vc.setDirection(Direction.SOUTH);
                }
                invalidateBoundingBox();
            }
        }

        if (e instanceof PropertyChangedEvent) {
            PropertyChangedEvent pc = (PropertyChangedEvent) e;
            String propertyName = pc.getPropertyName();
            if (propertyName.equals(Contact.PROPERTY_NAME)
                    || propertyName.equals(Contact.PROPERTY_IO_TYPE)
                    || propertyName.equals(VisualContact.PROPERTY_DIRECTION)) {

                invalidateBoundingBox();
                contactLableGlyphs.clear();
            }
        }
    }

    @Override
    public void addObserver(HierarchyObserver obs) {
        groupImpl.addObserver(obs);
    }

    @Override
    public void removeObserver(HierarchyObserver obs) {
        groupImpl.removeObserver(obs);
    }

    @Override
    public void removeAllObservers() {
        groupImpl.removeAllObservers();
    }

    @Override
    public void copyStyle(Stylable src) {
        super.copyStyle(src);
        if (src instanceof VisualCircuitComponent) {
            VisualCircuitComponent srcComponent = (VisualCircuitComponent) src;
            setIsEnvironment(srcComponent.getIsEnvironment());
        }
    }

    @Override
    public String getLabel() {
        return getReferencedComponent().getModule();
    }

    @Override
    public void setLabel(String label) {
        getReferencedComponent().setModule(label);
        super.setLabel(label);
    }

    public Collection<VisualContact> getVisualContacts() {
        return Hierarchy.filterNodesByType(getChildren(), VisualContact.class);
    }

    public List<VisualContact> getVisualInputs() {
        ArrayList<VisualContact> result = new ArrayList<>();
        for (VisualContact contact: getVisualContacts()) {
            if (contact.isInput()) {
                result.add(contact);
            }
        }
        return result;
    }

    public Collection<VisualContact> getVisualOutputs() {
        ArrayList<VisualContact> result = new ArrayList<>();
        for (VisualContact contact: getVisualContacts()) {
            if (contact.isOutput()) {
                result.add(contact);
            }
        }
        return result;
    }

    public VisualContact getFirstVisualInput() {
        VisualContact result = null;
        for (VisualContact contact: getVisualContacts()) {
            if (contact.isInput()) {
                result = contact;
                break;
            }
        }
        return result;
    }

    public VisualContact getFirstVisualOutput() {
        VisualContact result = null;
        for (VisualContact contact: getVisualContacts()) {
            if (contact.isOutput()) {
                result = contact;
                break;
            }
        }
        return result;
    }

    public VisualContact getMainVisualOutput() {
        VisualContact result = null;
        Collection<VisualContact> outputs = getVisualOutputs();
        if (outputs.size() == 1) {
            result = outputs.iterator().next();
        }
        return result;
    }

}
