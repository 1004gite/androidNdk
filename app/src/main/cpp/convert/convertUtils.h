//
// Created by 김기태 on 2022/06/24.
//

#ifndef ANDROIDNDK_CONVERTUTILS_H
#define ANDROIDNDK_CONVERTUTILS_H

struct rgb{
    double r;       // a fraction between 0 and 1
    double g;       // a fraction between 0 and 1
    double b;       // a fraction between 0 and 1
    rgb(double r, double g, double b) : r(r), g(g), b(b) {}
};

struct hsv{
    double h;       // angle in degrees
    double s;       // a fraction between 0 and 1
    double v;       // a fraction between 0 and 1
    hsv(double h, double s, double v) : h(h), s(s), v(v) {}
};

hsv rgb2hsv(rgb in);
rgb hsv2rgb(hsv in);

#endif //ANDROIDNDK_CONVERTUTILS_H
