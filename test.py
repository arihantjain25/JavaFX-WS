from flask import Flask
import psycopg2
import json

app = Flask(__name__)

host = 'localhost'
database = 'symbiota2'
user = 'postgres'
password = 'password'
port = '5432'
query = '''SELECT DISTINCT taxavernaculars."VernacularName", users."uid", taxa."RankId", tmtraits."traitid"
FROM taxa 
LEFT JOIN users ON taxa."modifiedUid" = users."uid"
LEFT JOIN taxavernaculars ON taxavernaculars."TID" = taxa."TID"
LEFT JOIN tmtraits ON users."uid" = tmtraits."createduid"
ORDER BY taxavernaculars."VernacularName", users."uid", taxa."RankId", tmtraits."traitid"'''
json_schema = '''{"vernacular_name":"","source":"","users":{"rank_id":""},"count":{"url":""}}'''
@app.route('/')
def hello_world():
    conn = psycopg2.connect(host=host, database=database, user=user, password=password, port=port)
    cur = conn.cursor()
    cur.execute(query)
    result = cur.fetchall()
    ugly_json = '['
    for row in result:
        temp = json_schema
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
    return pretty_json


if __name__ == '__main__':
    app.run()