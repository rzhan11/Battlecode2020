import math
import sys

cur_zone = (-1, -1)

def eval(x):
    return 2.7182818284**(0.0028 * x - 1.38 * math.sin(0.00157 * x - 1.73) + 1.38 * math.sin(-1.73)) - 1

max_height = 1000

print("switch(elevation){")
for elevation in range(max_height):
    val = -1
    for i in range(10000):
        if eval(i) > elevation:
            val = i
            break
    if val == -1:
        print("case " + str(elevation) + ":return " + str(1000000000) + ";", end="")
    else:
        print("case " + str(elevation) + ":return " + str(val) + ";", end="")


print("}")

# print("length:", len(array))
