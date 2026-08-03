package com.qrh.youshangdache.common.util;

import java.math.BigDecimal;

public class LocationUtil {

    // 地球赤道半径
    private static double EARTH_RADIUS = 6378.137;

    /**
     * 等同于Math.toRadians()
     *
     * @param d
     * @return
     */
    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * 等同于Math.toRadians()
     *
     * @param d
     * @return
     */
    private static BigDecimal rad(BigDecimal d) {
        return d.multiply(new BigDecimal(Double.toString(Math.PI)))
                .divide(new BigDecimal("180.0"));
    }

    /**
     * 根据两点经纬度坐标计算两点间的距离，单位为米
     *
     * @param lat1 起始点经度
     * @param lng2 起始点纬度
     * @param lat2 终点经度
     * @param lng2 终点纬度
     * @return 两点间的距离
     **/
    public static double getDistance(double lat1, double lng1, double lat2,
                                     double lng2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000d) / 10000d;
        s = s * 1000;
        return s;
    }

    /**
     * 根据两点经纬度坐标计算两点间的距离，单位为米
     *
     * @param lat1 起始点经度
     * @param lng2 起始点纬度
     * @param lat2 终点经度
     * @param lng2 终点纬度
     * @return 两点间的距离
     **/
    public static double getDistance(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2,
                                     BigDecimal lng2) {

        BigDecimal radLat1 = rad(lat1);
        BigDecimal radLat2 = rad(lat2);
        BigDecimal a = rad(lat1).subtract(rad(lat2));
        BigDecimal b = rad(lng1).subtract(rad(lng2));
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a.divide(new BigDecimal("2")).doubleValue()), 2)
                + Math.cos(radLat1.doubleValue()) * Math.cos(radLat2.doubleValue())
                * Math.pow(Math.sin(b.divide(new BigDecimal("2")).doubleValue()), 2)));
//        s = s * EARTH_RADIUS;
//        s = Math.round(s * 10000d) / 10000d;
//        s = s * 1000;

        return (Math.round(s * EARTH_RADIUS * 10_000d) / 10_000d) * 1_000d;
    }

    public static void main(String[] args) {
        double distance = getDistance(30.57404, 104.073013,
                30.509376, 104.077001);
        System.out.println("距离" + distance + "米");
    }

}
