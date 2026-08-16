#pragma once

#include "DataTypes.h"
#include <vector>

namespace HexAudio {

class HexMath {
public:
    struct Point {
        double x;
        double y;
    };

    // Convert cube/axial coordinates to pixel coordinates
    static Point cubeToPixel(const HexCoordinate& hex, double size);

    // Convert pixel coordinates to cube/axial coordinates
    static HexCoordinate pixelToCube(double x, double y, double size);

    // Round fractional cube coordinates to the nearest valid hex
    static HexCoordinate roundCube(double fracQ, double fracR, double fracS);

    // Generate a ring of hexes at a given radius
    static std::vector<HexCoordinate> generateRing(int radius);
};

} // namespace HexAudio
