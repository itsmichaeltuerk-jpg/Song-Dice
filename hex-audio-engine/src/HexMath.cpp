#include "HexMath.h"
#include <cmath>

namespace HexAudio {

HexMath::Point HexMath::cubeToPixel(const HexCoordinate& hex, double size) {
    double x = size * (std::sqrt(3.0) * hex.q + (std::sqrt(3.0) / 2.0) * hex.r);
    double y = size * ((3.0 / 2.0) * hex.r);
    return {x, y};
}

HexCoordinate HexMath::pixelToCube(double x, double y, double size) {
    double q = ((std::sqrt(3.0) / 3.0) * x - (1.0 / 3.0) * y) / size;
    double r = ((2.0 / 3.0) * y) / size;
    return roundCube(q, r, -q - r);
}

HexCoordinate HexMath::roundCube(double fracQ, double fracR, double fracS) {
    int q = std::round(fracQ);
    int r = std::round(fracR);
    int s = std::round(fracS);

    double qDiff = std::abs(q - fracQ);
    double rDiff = std::abs(r - fracR);
    double sDiff = std::abs(s - fracS);

    if (qDiff > rDiff && qDiff > sDiff) {
        q = -r - s;
    } else if (rDiff > sDiff) {
        r = -q - s;
    } else {
        s = -q - r;
    }
    return {q, r, s};
}

std::vector<HexCoordinate> HexMath::generateRing(int radius) {
    std::vector<HexCoordinate> results;
    if (radius == 0) {
        results.push_back({0, 0, 0});
        return results;
    }

    std::vector<HexCoordinate> directions = {
        {1, -1, 0}, {1, 0, -1}, {0, 1, -1},
        {-1, 1, 0}, {-1, 0, 1}, {0, -1, 1}
    };

    HexCoordinate hex = {
        directions[4].q * radius,
        directions[4].r * radius,
        directions[4].s * radius
    };

    for (int i = 0; i < 6; i++) {
        for (int j = 0; j < radius; j++) {
            results.push_back(hex);
            hex.q += directions[i].q;
            hex.r += directions[i].r;
            hex.s += directions[i].s;
        }
    }
    return results;
}

} // namespace HexAudio
