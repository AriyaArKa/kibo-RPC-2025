package jp.jaxa.iss.kibo.rpc.sampleapk.common;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.sampleapk.common.enumeration.CartesianDirection;

/**
 * @author Hashib Islam (#)
 * @since 2025-04-24
 */

public class QuaternionPoint {
    private Point point;
    private Quaternion quaternion;
    private CartesianDirection cartesianDirection;

    public QuaternionPoint() {
    }

    public QuaternionPoint(Point point, Quaternion quaternion) {
        this.point = point;
        this.quaternion = quaternion;
    }

    public QuaternionPoint(Point point, Quaternion quaternion, CartesianDirection cartesianDirection) {
        this.point = point;
        this.quaternion = quaternion;
        this.cartesianDirection = cartesianDirection;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public Quaternion getQuaternion() {
        return quaternion;
    }

    public void setQuaternion(Quaternion quaternion) {
        this.quaternion = quaternion;
    }

    public CartesianDirection getCartesianDirection() {
        return cartesianDirection;
    }

    public void setCartesianDirection(CartesianDirection cartesianDirection) {
        this.cartesianDirection = cartesianDirection;
    }

    @Override
    public String toString() {
        return "QuaternionPoint{" +
                "point=" + point +
                ", quaternion=" + quaternion +
                ", direction=" + cartesianDirection +
                '}';
    }
}
