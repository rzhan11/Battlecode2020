import math
import sys

cur_zone = (-1, -1)

def mag(a):
    return (a[0]-cur_zone[0])**2 + (a[1]-cur_zone[1])**2


num_zones = int(sys.argv[1])

array = [[] for i in range(num_zones * num_zones)]

print("switch(zoneIndex){")
# s_first = True
for sx in range(1):
    for sy in range(1):
        index = sx * num_zones + sy
        cur_zone = (sx, sy)
        for ex in range(num_zones):
            for ey in range(num_zones):
                array[index] += [(ex, ey, ex**2 + ey**2)]
        array[index].sort(key=mag)
        print("case " + str(index) + ":return new int[][]", end="")
        print("{", end="")
        e_first = True
        for p in array[index]:
            if not e_first:
                print(",", end="")
            e_first = False

            print("{"+str(p[0])+","+str(p[1])+"}", end="")
        print("};", end="\n")

print("}")

# print("length:", len(array))
