with open("keywords.txt") as file:
    temp = file.read().split("\n");
    keywords = []
    for i in range(len(temp)):
        if temp[i].strip() != "":
            keywords += [temp[i]]

print("keywords:", str(keywords))

with open("log.txt", "r") as file:
    lines = file.read().split("\n")
print("log.txt has", len(lines), "lines")


index = 0
while index < len(lines):
    if "Match Starting" in lines[index]:
        seenEvents = set()
        matchName = lines[index + 1][9:]
        print(matchName)
        index += 2
        myType = "none"
        roundNum = "none"
        here = "none"
        while index < len(lines):
            if "Match Finished" in lines[index]:
                break
            if "-Robot: " in lines[index]:
                myType = lines[index][8:]
            if "-roundNum: " in lines[index]:
                roundNum = lines[index][11:]
            if "-Location: " in lines[index]:
                location = lines[index][11:]
            for k in keywords:
                if k in lines[index]:
                    temp = (k, myType)
                    if temp in seenEvents:
                        continue
                    seenEvents.add(temp)
                    print("\tFound keyword", "'" + k + "'")
                    print("\t\tmyType:", myType)
                    print("\t\troundNum:", roundNum)
                    print("\t\tlocation:", location)
            index += 1
    index += 1
