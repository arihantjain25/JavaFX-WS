from flask import Flask
import psycopg2
import json

app = Flask(__name__)

host = 'localhost'
database = 'symbiota2'
user = 'postgres'
password = 'password'
query = '''SELECT DISTINCT taxavernaculars."VernacularName", users."uid", taxa."RankId", tmtraits."traitid"
    FROM taxa 
    LEFT JOIN taxavernaculars ON taxavernaculars."TID" = taxa."TID"
    LEFT JOIN tmtraittaxalink ON taxa."TID" = tmtraittaxalink."tid"
    LEFT JOIN users ON taxa."modifiedUid" = users."uid"
    LEFT JOIN tmtraits ON tmtraittaxalink."traitid" = tmtraits."traitid"
    ORDER BY taxavernaculars."VernacularName", users."uid", taxa."RankId", tmtraits."traitid"'''


@app.route('/')
def hello_world():
    conn = psycopg2.connect(host=host, database=database, user=user, password=password)
    cur = conn.cursor()
    cur.execute(query)
    result = cur.fetchall()
    sk_schema = ''
    ugly_json = '['

    with open('generatedfiles/schema.json', 'r') as file:
        json_schema = file.read()
    schema_split = json_schema.split()

    for i in range(len(schema_split)):
        if ':' in schema_split[i]:
            if '"' in schema_split[i + 1]:
                if ',' in schema_split[i + 1]:
                    schema_split[i + 1] = '"",'
                else:
                    schema_split[i + 1] = '""'

    for s in schema_split:
        sk_schema = sk_schema + s

    for row in result:
        temp = sk_schema
        for r in row:
            if r is None:
                temp = temp.replace('""', 'null', 1)
            else:
                temp = temp.replace('""', '"' + str(r) + '"', 1)
        ugly_json = ugly_json + temp + ','

    ugly_json = ugly_json[:-1]
    ugly_json = ugly_json + ']'
    parsed = json.loads(ugly_json)
    pretty_json = json.dumps(parsed, indent=2)
    print(pretty_json)
    return pretty_json


if __name__ == '__main__':
    app.run()
