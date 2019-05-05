import json

with open('demooutput.txt', 'r') as file:
    uglyjson = file.read()
parsed = json.loads(uglyjson)
prettyjson = json.dumps(parsed, indent=2)
f = open('demooutput.txt', 'r+')
f.truncate(0)
f.write(prettyjson)
f.close()
