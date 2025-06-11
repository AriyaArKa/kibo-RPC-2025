package jp.jaxa.iss.kibo.rpc.sampleapk.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.sampleapk.common.enumeration.AreaEnum;
import jp.jaxa.iss.kibo.rpc.sampleapk.common.enumeration.CartesianDirection;

/**
 * @author Hashib Islam (#)
 * @since 2025-04-24
 */

public class Constants {
    public static final int MAX_RETRIES = 3;
    public static final int RETRY_WAIT_MS = 500;
    public static final int MOVE_TO_COMPLETE_WAIT_MS = 1500;
    public static final int CAM_STABILIZATION_WAIT_MS = 2000;
    public static final boolean DEBUG_MODE = true;
    public static final  List<AreaEnum> AREA_LIST;
    public static final Map<AreaEnum, QuaternionPoint> LOST_ITEM_SEARCH_PLANE_PER_AREA;

    public static final QuaternionPoint AREA_1_COORDINATES = new QuaternionPoint(
            new Point(11d, -9.8d, 4.8d),
            new Quaternion(0f, 0f, -0.707f, 0.707f)
    );

    public static final QuaternionPoint AREA_2_COORDINATES = new QuaternionPoint(
            new Point(11d, -9.1d, 5.2),
            new Quaternion(0f, 0f, -0.707f, 0.707f)
    );

    public static final QuaternionPoint AREA_3_COORDINATES = new QuaternionPoint(
            new Point(10.7d, -8.1d, 5.2d),
            new Quaternion(0f, 0f, -0.707f, 0.707f)
    );

    public static final QuaternionPoint AREA_4_COORDINATES = new QuaternionPoint(
            new Point(11.1, -7d, 4.7d),
            new Quaternion(0f, 0f, -0.707f, 0.707f)
    );

    public static final Map<AreaEnum, QuaternionPoint> AREA_COORDINATES_MAP;

    static {
        Map<AreaEnum, QuaternionPoint> map = new HashMap<>();

        map.put(AreaEnum.AREA_1, AREA_1_COORDINATES);
        map.put(AreaEnum.AREA_2, AREA_2_COORDINATES);
        map.put(AreaEnum.AREA_3, AREA_3_COORDINATES);
        map.put(AreaEnum.AREA_4, AREA_4_COORDINATES);

        AREA_COORDINATES_MAP = Collections.unmodifiableMap(map);
    }

    static {
        List<AreaEnum> list = new ArrayList<>();

        list.add(AreaEnum.AREA_1);
        list.add(AreaEnum.AREA_2);
        list.add(AreaEnum.AREA_3);
        list.add(AreaEnum.AREA_4);

        AREA_LIST = Collections.unmodifiableList(list);
    }

    static {
        //     [ xMin,    yMin,   zMin,    xMax,    yMax,   zMax ]
        double[][] BOUNDS = {
                {10.42,  -10.58,  4.82,    11.48,  -10.58,  5.57  },   // AREA_1
                {10.30,   -9.25,  3.76203, 11.55,  -8.50,   3.76203},  // AREA_2
                {10.30,   -8.40,  3.76093, 11.55,  -7.45,   3.76093},  // AREA_3
                {9.866984, -7.34, 4.32,    9.866984, -6.365, 5.57  }   // AREA_4
        };

        Map<AreaEnum, QuaternionPoint> map = new EnumMap<>(AreaEnum.class);
        for (AreaEnum area : AreaEnum.values()) {
            double[] bounds = BOUNDS[area.ordinal()];
            double xMin = bounds[0], yMin = bounds[1], zMin = bounds[2];
            double xMax = bounds[3], yMax = bounds[4], zMax = bounds[5];

            // calc center
            Point center = new Point(
                    (xMin + xMax) / 2.0,
                    (yMin + yMax) / 2.0,
                    (zMin + zMax) / 2.0
            );

            // store enum orientation
            CartesianDirection cartesianDirection;

            // choose orientation based on axis
            Quaternion orientation;

            // y constant == target orientation
            if (yMin == yMax) {
                // face -Y based on rule book
                orientation = new Quaternion(0f, 0f, -0.7071068f, 0.7071068f);
                cartesianDirection = CartesianDirection.Y_NEG;
            }
            // x constant == target orientation
            else if (xMin == xMax) {
                // face -X based on rule book
                orientation = new Quaternion(0f, 0f,  1.0f,0f);
                cartesianDirection = CartesianDirection.X_NEG;
            }
            // z constant == target orientation
            else if (zMin == zMax) {
                // face -Z based on rule book
                orientation = new Quaternion(0f,  0.7071068f, 0f, 0.7071068f);
                cartesianDirection = CartesianDirection.Z_POS;
            }
            // default
            else {
                orientation = new Quaternion(0f, 0f, 0f, 1f);
                cartesianDirection = CartesianDirection.X_POS;
            }

            map.put(area, new QuaternionPoint(center, orientation, cartesianDirection));
        }

        LOST_ITEM_SEARCH_PLANE_PER_AREA = Collections.unmodifiableMap(map);
    }
}
