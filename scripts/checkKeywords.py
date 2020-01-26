with open("keywords.txt") as file:
    temp = file.read().split("\n")
    keywords = []
    for i in range(len(temp)):
        if temp[i].strip() != "":
            keywords += [temp[i]]

print("keywords:", str(keywords))

with open("log.txt", "r") as file:
    lines = file.read().split("\n")
print("log.txt has", len(lines), "lines")


def isHeader(line):
    return all(c in line for c in "[:#@]")


index = 0
while index < len(lines):
    if "Match Starting" in lines[index]:
        seenEvents = set()
        matchName = lines[index + 1][9:]
        print(matchName)
        index += 2
        header = "none"
        while index < len(lines):
            if "Match Finished" in lines[index]:
                break
            if isHeader(lines[index]):
                header = lines[index][:lines[index].index("]") + 1]
                myTeam = header[header.index("[") + 1:header.index(":")]
                myType = header[header.index(":") + 1:header.index("#")]
                id = header[header.index("#") + 1:header.index("@")]
                roundNum = header[header.index("@") + 1:header.index("]")]

            for k in keywords:
                if k in lines[index]:
                    temp = (k, myType, myTeam)
                    if temp in seenEvents:
                        continue
                    seenEvents.add(temp)
                    print("\tFound keyword", "'" + k + "' on line", index)
                    print("\t\theader:", header)
                    print("\t\tmyTeam:", myTeam)
                    print("\t\tmyType:", myType)
                    print("\t\tid:", id)
                    print("\t\troundNum:", roundNum)
            index += 1
    index += 1
