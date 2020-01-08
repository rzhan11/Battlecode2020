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
    matchName = lines[index + 1][9:]
    print(matchName)
    index += 2
    roundNum = -1
    while index < len(lines):
      if "Match Finished" in lines[index]:
          break
      if "-roundNum: " in lines[index]:
          roundNum = lines[index][11:]
      for k in keywords:
        if k in lines[index]:
          print("\tFound keyword", "'" + k + "'", "at roundNum", roundNum, "on line", index)
      index += 1
  index += 1
