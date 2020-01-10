import math
import sys


def mag(a):
    return a[0] * a[0] + a[1] * a[1]


radius_squared = int(sys.argv[1])
max_radius = math.ceil(math.sqrt(radius_squared))

array = []

for x in range(-max_radius, max_radius + 1):
    for y in range(-max_radius, max_radius + 1):
        if mag((x, y)) <= radius_squared:
            array += [(x, y, x * x + y * y)]

array.sort(key=mag)

first = True
for pair in array:
    if first:
        print("{", end="")
        first = False
    else:
        print(",", end="")
    print("{" + str(pair[0]) + "," + str(pair[1]) + "," + str(pair[2]) + "}", end="")
print("}")

print("length:", len(array))
